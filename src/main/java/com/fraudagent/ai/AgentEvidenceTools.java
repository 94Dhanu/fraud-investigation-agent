package com.fraudagent.ai;

import com.fraudagent.domain.model.CaseFacts;
import com.fraudagent.domain.model.EvidenceItem;
import com.fraudagent.domain.model.PolicyCitation;
import com.fraudagent.domain.port.EvidenceTool;
import com.fraudagent.service.CaseFactsProvider;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Spring AI tool-calling surface. Exposes the six deterministic evidence
 * tools plus policy-corpus retrieval to the LLM agent. Each method is a thin
 * adapter: it resolves {@link CaseFacts} for the case and delegates to the
 * framework-neutral {@link EvidenceTool} implementation, so AI mode and
 * rules mode compute identical evidence.
 */
@Component
public class AgentEvidenceTools {

    private final CaseFactsProvider factsProvider;
    private final PolicyRagService policyRagService;
    private final Map<String, EvidenceTool> toolsByName;

    public AgentEvidenceTools(CaseFactsProvider factsProvider,
                              PolicyRagService policyRagService,
                              List<EvidenceTool> evidenceTools) {
        this.factsProvider = factsProvider;
        this.policyRagService = policyRagService;
        this.toolsByName = evidenceTools.stream()
                .collect(Collectors.toMap(EvidenceTool::toolName, Function.identity()));
    }

    public List<EvidenceItem> runAll(Long caseId) {
        CaseFacts facts = factsProvider.factsFor(caseId);
        return toolsByName.values().stream()
                .map(tool -> tool.run(facts))
                .toList();
    }

    private EvidenceItem invoke(String toolName, Long caseId) {
        EvidenceTool tool = toolsByName.get(toolName);
        if (tool == null) {
            throw new IllegalArgumentException("Unknown evidence tool: " + toolName);
        }
        return tool.run(factsProvider.factsFor(caseId));
    }

    @Tool(description = "Check transaction velocity for the card: authorisations in the "
            + "last 1 hour and 24 hours, and deviation of the disputed amount from the "
            + "cardholder's 30-day average ticket size.")
    public EvidenceItem transactionVelocityCheck(
            @ToolParam(description = "The numeric fraud case ID") Long caseId) {
        return invoke("transaction_velocity_check", caseId);
    }

    @Tool(description = "Analyse the transaction's geo-location vs the cardholder's home "
            + "region: cross-border usage, distance, and impossible-travel detection.")
    public EvidenceItem geoLocationAnomaly(
            @ToolParam(description = "The numeric fraud case ID") Long caseId) {
        return invoke("geo_location_anomaly", caseId);
    }

    @Tool(description = "Evaluate the device fingerprint behind the transaction: known vs "
            + "new device, device age, emulator/proxy indicators and prior fraud reports.")
    public EvidenceItem deviceFingerprintReputation(
            @ToolParam(description = "The numeric fraud case ID") Long caseId) {
        return invoke("device_fingerprint_reputation", caseId);
    }

    @Tool(description = "Look up the merchant's risk profile: merchant-category risk band, "
            + "90-day chargeback rate, acquirer tier and internal watchlist membership.")
    public EvidenceItem merchantRiskProfile(
            @ToolParam(description = "The numeric fraud case ID") Long caseId) {
        return invoke("merchant_risk_profile", caseId);
    }

    @Tool(description = "Detect card-testing behaviour: small-value declined authorisations "
            + "preceding the disputed transaction.")
    public EvidenceItem cardTestingPattern(
            @ToolParam(description = "The numeric fraud case ID") Long caseId) {
        return invoke("card_testing_pattern", caseId);
    }

    @Tool(description = "Review the cardholder's account history: account age, prior "
            + "disputes, confirmed fraud, and recent credential/contact changes "
            + "(account-takeover indicators).")
    public EvidenceItem accountHistoryStanding(
            @ToolParam(description = "The numeric fraud case ID") Long caseId) {
        return invoke("account_history_standing", caseId);
    }

    @Tool(description = "Search the bank's fraud and dispute policy corpus. Returns the "
            + "matching policy sections with exact document IDs and verbatim excerpts "
            + "that MUST be cited in the final verdict.")
    public List<PolicyCitation> policyLookup(
            @ToolParam(description = "Natural language search query, e.g. 'impossible travel' "
                    + "or 'card testing thresholds'") String query) {
        return policyRagService.retrieve(query);
    }
}
