package com.fraudagent.ai;

import java.util.List;

/**
 * Structured output schema the LLM agent is asked to produce. Citations are
 * returned as policy document IDs only — the real {@code PolicyCitation}
 * objects are resolved from retrieved documents so the model can never
 * fabricate a citation.
 */
public record AgentVerdict(
        int riskScore,
        String riskLevel,
        String recommendedAction,
        List<String> keyFactors,
        String rationale,
        List<String> citedPolicyIds
) {
}
