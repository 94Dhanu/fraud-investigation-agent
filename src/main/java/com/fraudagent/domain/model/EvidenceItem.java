package com.fraudagent.domain.model;

import java.util.Map;

/**
 * Output of a single deterministic evidence tool.
 *
 * @param riskContribution 0..100 contribution to the overall risk score
 */
public record EvidenceItem(
        String toolName,
        String summary,
        int riskContribution,
        Map<String, String> details
) {
}
