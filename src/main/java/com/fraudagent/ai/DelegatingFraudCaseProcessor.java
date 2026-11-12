package com.fraudagent.ai;

import com.fraudagent.domain.model.CaseFacts;
import com.fraudagent.domain.model.EvidenceItem;
import com.fraudagent.domain.model.FraudAssessment;
import com.fraudagent.domain.model.ProcessorUsed;
import com.fraudagent.domain.port.FraudCaseProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Primary {@link FraudCaseProcessor} used by the application layer. Routes
 * to the Spring AI agent when it is registered ({@code fraud.agent.mode=ai});
 * any AI failure (no chat model, API error, malformed output) degrades
 * gracefully to the deterministic rules engine, marked RULES_FALLBACK.
 */
@Primary
@Component
public class DelegatingFraudCaseProcessor implements FraudCaseProcessor {

    private static final Logger log = LoggerFactory.getLogger(DelegatingFraudCaseProcessor.class);

    private final RulesFraudCaseProcessor rulesProcessor;
    private final ObjectProvider<SpringAiFraudCaseProcessor> aiProcessorProvider;

    public DelegatingFraudCaseProcessor(RulesFraudCaseProcessor rulesProcessor,
                                        ObjectProvider<SpringAiFraudCaseProcessor> aiProcessorProvider) {
        this.rulesProcessor = rulesProcessor;
        this.aiProcessorProvider = aiProcessorProvider;
    }

    @Override
    public FraudAssessment assess(CaseFacts facts, List<EvidenceItem> evidence) {
        SpringAiFraudCaseProcessor ai = aiProcessorProvider.getIfAvailable();
        if (ai != null) {
            try {
                return ai.assess(facts, evidence);
            } catch (Exception e) {
                log.warn("AI agent failed for case {} ({}); falling back to rules engine",
                        facts.caseId(), e.getMessage());
                return rulesProcessor.assess(facts, evidence, ProcessorUsed.RULES_FALLBACK,
                        "AI agent unavailable (" + e.getMessage() + ") — deterministic rules fallback applied");
            }
        }
        return rulesProcessor.assess(facts, evidence);
    }
}
