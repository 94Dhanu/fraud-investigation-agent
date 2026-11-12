package com.fraudagent.domain.model;

import java.util.List;

/**
 * Structured fraud-risk recommendation produced by a {@code FraudCaseProcessor}.
 * This is the contract between the AI/rules layer and the rest of the system.
 */
public record FraudAssessment(
        int riskScore,
        RiskLevel riskLevel,
        RecommendedAction recommendedAction,
        List<String> keyFactors,
        String rationale,
        List<PolicyCitation> policyCitations,
        ProcessorUsed processorUsed
) {
}
