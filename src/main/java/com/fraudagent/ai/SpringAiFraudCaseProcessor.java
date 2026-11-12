package com.fraudagent.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fraudagent.domain.model.CaseFacts;
import com.fraudagent.domain.model.EvidenceItem;
import com.fraudagent.domain.model.FraudAssessment;
import com.fraudagent.domain.model.PolicyCitation;
import com.fraudagent.domain.model.ProcessorUsed;
import com.fraudagent.domain.model.RecommendedAction;
import com.fraudagent.domain.model.RiskLevel;
import com.fraudagent.domain.port.FraudCaseProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * LLM-backed fraud investigation agent (Spring AI):
 * <ul>
 *   <li>tool calling against the six deterministic evidence tools + policy lookup</li>
 *   <li>case-isolated conversation memory ({@code conversationId = case-{id}})</li>
 *   <li>structured output via {@link AgentVerdict}; citations are resolved from
 *       retrieved policy documents so they are always exact</li>
 * </ul>
 * Only registered when {@code fraud.agent.mode=ai}. Any failure inside
 * {@link #assess} propagates so the router can fall back to rules mode.
 */
@Component
@ConditionalOnProperty(name = "fraud.agent.mode", havingValue = "ai")
public class SpringAiFraudCaseProcessor implements FraudCaseProcessor {

    private static final Logger log = LoggerFactory.getLogger(SpringAiFraudCaseProcessor.class);

    private static final String INVESTIGATOR_SYSTEM = """
            You are a senior payment-fraud investigator for a bank. You investigate
            card dispute cases using ONLY the evidence tools provided — never guess
            evidence. Steps:
            1. Call ALL SIX evidence tools for the case, plus policyLookup for any
               policy sections you need.
            2. Weigh the evidence: impossible travel, card-testing patterns,
               watchlisted merchants, hostile devices and account-takeover signals
               are strong corroboration of unauthorized use. Recognised devices,
               normal velocity and serial-disputer history suggest first-party fraud.
            3. Apply the POLICY CONTEXT exactly: mandatory escalation conditions,
             denial criteria and review requirements are defined there.
            Write a concise investigative analysis, then stop.""";

    private static final String EXTRACTION_SYSTEM = """
            Based on your investigation, produce the final structured verdict.
            Rules:
            - riskScore: integer 0-100 = strength of corroborating evidence that the
              charge was genuinely fraudulent (higher = more fraud evidence).
            - riskLevel: one of LOW, MEDIUM, HIGH, CRITICAL.
            - recommendedAction: one of APPROVE_DISPUTE_REFUND, DENY_DISPUTE,
              ESCALATE_TO_SENIOR_INVESTIGATOR, REQUEST_ADDITIONAL_INFORMATION.
            - keyFactors: 3-6 short bullet strings naming the decisive evidence.
            - rationale: 2-4 sentences justifying the action, citing policy sections.
            - citedPolicyIds: the policy documentIds (e.g. "POL-305") you relied on —
              only IDs from the POLICY CONTEXT or policyLookup results.""";

    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;
    private final ChatMemory chatMemory;
    private final PolicyRagService policyRagService;
    private final ToolCallback[] toolCallbacks;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public SpringAiFraudCaseProcessor(ObjectProvider<ChatClient.Builder> chatClientBuilderProvider,
                                      ChatMemory chatMemory,
                                      PolicyRagService policyRagService,
                                      AgentEvidenceTools agentEvidenceTools) {
        this.chatClientBuilderProvider = chatClientBuilderProvider;
        this.chatMemory = chatMemory;
        this.policyRagService = policyRagService;
        this.toolCallbacks = ToolCallbacks.from(agentEvidenceTools);
    }

    public static String conversationIdFor(Long caseId) {
        return "case-" + caseId;
    }

    @Override
    public FraudAssessment assess(CaseFacts facts, List<EvidenceItem> evidence) {
        ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
        if (builder == null) {
            throw new IllegalStateException("ChatClient.Builder unavailable — no chat model configured");
        }
        ChatClient chatClient = builder
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
        String conversationId = conversationIdFor(facts.caseId());
        List<PolicyCitation> retrieved = policyRagService.retrieveForCase(facts, evidence);

        String analysis = chatClient.prompt()
                .system(INVESTIGATOR_SYSTEM)
                .user(investigationPrompt(facts, retrieved))
                .toolCallbacks(toolCallbacks)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();
        log.debug("Case {} agent analysis: {}", facts.caseId(), analysis);

        AgentVerdict verdict = chatClient.prompt()
                .system(EXTRACTION_SYSTEM)
                .user("Produce the structured verdict for case " + facts.caseId() + " now.")
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .entity(AgentVerdict.class);
        if (verdict == null) {
            throw new IllegalStateException("LLM returned no structured verdict");
        }
        return toAssessment(verdict, retrieved);
    }

    private String investigationPrompt(CaseFacts facts, List<PolicyCitation> retrieved) {
        StringBuilder sb = new StringBuilder();
        sb.append("Investigate fraud case ID ").append(facts.caseId())
                .append(" for transaction ").append(facts.transactionId()).append(".\n\n")
                .append("CASE FACTS:\n");
        try {
            sb.append(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(facts));
        } catch (Exception e) {
            sb.append(facts.toString());
        }
        sb.append("\nPOLICY CONTEXT (retrieved sections — cite these documentIds):\n");
        for (PolicyCitation c : retrieved) {
            sb.append("[").append(c.documentId()).append(" | ").append(c.section()).append("]\n")
                    .append(c.excerpt()).append("\n\n");
        }
        sb.append("Call the six evidence tools with caseId=").append(facts.caseId())
                .append(", then write your analysis.");
        return sb.toString();
    }

    private FraudAssessment toAssessment(AgentVerdict verdict, List<PolicyCitation> retrieved) {
        int score = Math.max(0, Math.min(100, verdict.riskScore()));
        RiskLevel riskLevel = parseEnum(RiskLevel.class, verdict.riskLevel(), score < 35
                ? RiskLevel.LOW : score < 55 ? RiskLevel.MEDIUM : score < 70 ? RiskLevel.HIGH : RiskLevel.CRITICAL);
        RecommendedAction action = parseEnum(RecommendedAction.class, verdict.recommendedAction(),
                score >= 70 ? RecommendedAction.ESCALATE_TO_SENIOR_INVESTIGATOR
                        : score < 35 ? RecommendedAction.DENY_DISPUTE
                        : RecommendedAction.APPROVE_DISPUTE_REFUND);

        List<String> keyFactors = verdict.keyFactors() == null || verdict.keyFactors().isEmpty()
                ? List.of("LLM assessment — see rationale")
                : new ArrayList<>(verdict.keyFactors());

        List<PolicyCitation> citations = resolveCitations(verdict.citedPolicyIds(), retrieved);
        String rationale = verdict.rationale() == null || verdict.rationale().isBlank()
                ? "LLM agent assessment (no rationale returned)."
                : verdict.rationale();

        return new FraudAssessment(score, riskLevel, action, keyFactors, rationale,
                citations, ProcessorUsed.AI_AGENT);
    }

    /**
     * Exact citations only: model-supplied documentIds are resolved against the
     * retrieved corpus. If the model cited nothing, the top retrieved sections
     * are attached so the recommendation is never citation-free.
     */
    private List<PolicyCitation> resolveCitations(List<String> citedIds, List<PolicyCitation> retrieved) {
        List<PolicyCitation> resolved = new ArrayList<>();
        if (citedIds != null) {
            for (String id : citedIds) {
                retrieved.stream()
                        .filter(c -> c.documentId().equalsIgnoreCase(id.trim()))
                        .findFirst()
                        .ifPresent(resolved::add);
            }
        }
        if (resolved.isEmpty()) {
            resolved.addAll(retrieved.stream().limit(2).toList());
        }
        return resolved;
    }

    private <E extends Enum<E>> E parseEnum(Class<E> type, String raw, E fallback) {
        if (raw == null) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, raw.trim().toUpperCase(Locale.ROOT)
                    .replace(' ', '_').replace('-', '_'));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
