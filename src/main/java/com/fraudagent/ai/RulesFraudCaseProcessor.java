package com.fraudagent.ai;

import com.fraudagent.config.FraudAgentProperties;
import com.fraudagent.domain.model.CaseFacts;
import com.fraudagent.domain.model.EvidenceItem;
import com.fraudagent.domain.model.FraudAssessment;
import com.fraudagent.domain.model.PolicyCitation;
import com.fraudagent.domain.model.ProcessorUsed;
import com.fraudagent.domain.model.RecommendedAction;
import com.fraudagent.domain.model.RiskLevel;
import com.fraudagent.domain.port.FraudCaseProcessor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Deterministic rules engine — the default processor and the runtime
 * fallback for the AI agent. Combines the six evidence tools' risk
 * contributions with policy-mandated escalation overrides (POL-515 §2).
 */
@Component
public class RulesFraudCaseProcessor implements FraudCaseProcessor {

    private static final Map<String, Double> TOOL_WEIGHTS = Map.of(
            "transaction_velocity_check", 1.0,
            "geo_location_anomaly", 1.2,
            "device_fingerprint_reputation", 1.2,
            "merchant_risk_profile", 0.9,
            "card_testing_pattern", 1.3,
            "account_history_standing", 1.0);

    private final PolicyRagService policyRagService;
    private final FraudAgentProperties properties;

    public RulesFraudCaseProcessor(PolicyRagService policyRagService,
                                   FraudAgentProperties properties) {
        this.policyRagService = policyRagService;
        this.properties = properties;
    }

    @Override
    public FraudAssessment assess(CaseFacts facts, List<EvidenceItem> evidence) {
        return assess(facts, evidence, ProcessorUsed.RULES_ENGINE, null);
    }

    /** Same deterministic assessment, but labelled for the processor actually used. */
    public FraudAssessment assess(CaseFacts facts, List<EvidenceItem> evidence,
                                ProcessorUsed processorUsed, String extraFactor) {
        int denyBelow = properties.riskThresholds().denyBelow();
        int escalateAt = properties.riskThresholds().escalateAtOrAbove();

        Map<String, String> details = mergedDetails(evidence);
        boolean escalationOverride = "true".equals(details.get("impossibleTravel"))
                || "true".equals(details.get("cardTestingPatternDetected"))
                || "true".equals(details.get("onInternalWatchlist"))
                || "true".equals(details.get("accountTakeoverSignal"));

        int score = weightedScore(evidence);
        if (escalationOverride) {
            score = Math.max(score, escalateAt);
        }
        score = Math.max(0, Math.min(100, score));

        RiskLevel riskLevel = score < denyBelow ? RiskLevel.LOW
                : score < 55 ? RiskLevel.MEDIUM
                : score < escalateAt ? RiskLevel.HIGH
                : RiskLevel.CRITICAL;

        boolean serialDisputer = parseInt(details.get("priorDisputes")) >= 3;
        RecommendedAction action;
        if (score >= escalateAt) {
            action = RecommendedAction.ESCALATE_TO_SENIOR_INVESTIGATOR;
        } else if (score < denyBelow) {
            action = serialDisputer
                    ? RecommendedAction.REQUEST_ADDITIONAL_INFORMATION   // POL-620 §2
                    : RecommendedAction.DENY_DISPUTE;                    // POL-620 §3
        } else {
            action = RecommendedAction.APPROVE_DISPUTE_REFUND;
        }

        List<String> keyFactors = keyFactors(evidence, escalationOverride, serialDisputer);
        if (extraFactor != null) {
            keyFactors = new ArrayList<>(keyFactors);
            keyFactors.add(0, extraFactor);
        }

        List<PolicyCitation> citations = policyRagService.retrieveForCase(facts, evidence);
        String rationale = rationale(score, riskLevel, action, evidence, escalationOverride, serialDisputer);

        return new FraudAssessment(score, riskLevel, action, keyFactors, rationale,
                citations, processorUsed);
    }

    private int weightedScore(List<EvidenceItem> evidence) {
        double weighted = 0;
        double weightSum = 0;
        for (EvidenceItem item : evidence) {
            double w = TOOL_WEIGHTS.getOrDefault(item.toolName(), 1.0);
            weighted += item.riskContribution() * w;
            weightSum += w;
        }
        return weightSum == 0 ? 0 : (int) Math.round(weighted / weightSum);
    }

    private Map<String, String> mergedDetails(List<EvidenceItem> evidence) {
        Map<String, String> merged = new LinkedHashMap<>();
        for (EvidenceItem item : evidence) {
            if (item.details() != null) {
                merged.putAll(item.details());
            }
        }
        return merged;
    }

    private List<String> keyFactors(List<EvidenceItem> evidence, boolean escalationOverride,
                                    boolean serialDisputer) {
        List<String> factors = new ArrayList<>();
        if (escalationOverride) {
            factors.add("Mandatory escalation condition met per POL-515 §2");
        }
        if (serialDisputer) {
            factors.add("Cardholder classified as serial disputer per POL-620 §2");
        }
        evidence.stream()
                .filter(e -> e.riskContribution() >= 20)
                .sorted(Comparator.comparingInt(EvidenceItem::riskContribution).reversed())
                .limit(4)
                .forEach(e -> factors.add(e.toolName() + " (risk " + e.riskContribution() + "): " + e.summary()));
        if (factors.isEmpty()) {
            factors.add("No strong fraud indicators across the six evidence dimensions");
        }
        return factors;
    }

    private String rationale(int score, RiskLevel riskLevel, RecommendedAction action,
                             List<EvidenceItem> evidence, boolean escalationOverride,
                             boolean serialDisputer) {
        String strongest = evidence.stream()
                .max(Comparator.comparingInt(EvidenceItem::riskContribution))
                .map(e -> e.toolName() + " contributed the highest individual risk (" + e.riskContribution() + ")")
                .orElse("no evidence collected");
        return String.format(
                "Deterministic rules assessment: weighted evidence score %d/100 (%s risk). "
                        + "%s. %sRecommended action %s per policy thresholds "
                        + "(deny below %d, escalate at %d).",
                score, riskLevel,
                escalationOverride
                        ? "A policy-mandated escalation flag was triggered"
                        : "No mandatory escalation flags were triggered",
                serialDisputer ? "Serial-disputer policy applied. " : "",
                action, properties.riskThresholds().denyBelow(),
                properties.riskThresholds().escalateAtOrAbove())
                + " Strongest signal: " + strongest + ".";
    }

    private int parseInt(String value) {
        try {
            return value == null ? 0 : Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
