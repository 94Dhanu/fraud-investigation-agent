package com.fraudagent.ai;

import com.fraudagent.config.FraudAgentProperties;
import com.fraudagent.domain.model.CaseFacts;
import com.fraudagent.domain.model.DisputeReason;
import com.fraudagent.domain.model.EvidenceItem;
import com.fraudagent.domain.model.FraudAssessment;
import com.fraudagent.domain.model.RecommendedAction;
import com.fraudagent.domain.model.RiskLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.SimpleVectorStore;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RulesFraudCaseProcessorTest {

    private RulesFraudCaseProcessor processor;

    @BeforeEach
    void setUp() throws Exception {
        PolicyRagService rag = new PolicyRagService(
                SimpleVectorStore.builder(new LocalHashingEmbeddingModel()).build(), 4);
        rag.indexPolicyCorpus();
        processor = new RulesFraudCaseProcessor(rag,
                new FraudAgentProperties("rules",
                        new FraudAgentProperties.RiskThresholds(35, 70)));
    }

    private CaseFacts facts() {
        return new CaseFacts(7L, "TXN-T1", "card_fp_t1", "ACC-1", "MERCH-1",
                "ELECTRONICS", new BigDecimal("300"), "USD", Instant.now().minusSeconds(3600),
                "US", "RO", "ECOMMERCE", DisputeReason.UNAUTHORIZED_TRANSACTION, "n");
    }

    private EvidenceItem item(String tool, int risk, String... kv) {
        Map<String, String> details = new HashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            details.put(kv[i], kv[i + 1]);
        }
        return new EvidenceItem(tool, tool + " summary", risk, details);
    }

    private List<EvidenceItem> evidenceOf(int risk) {
        return evidenceOf(risk, Map.of());
    }

    private List<EvidenceItem> evidenceOf(int risk, Map<String, String> extraDetails) {
        List<EvidenceItem> base = new java.util.ArrayList<>(List.of(
                item("transaction_velocity_check", risk),
                item("geo_location_anomaly", risk),
                item("device_fingerprint_reputation", risk),
                item("merchant_risk_profile", risk),
                item("card_testing_pattern", risk)));
        String[] kv = extraDetails.entrySet().stream()
                .flatMap(e -> java.util.stream.Stream.of(e.getKey(), e.getValue()))
                .toArray(String[]::new);
        base.add(item("account_history_standing", risk, kv));
        return base;
    }

    @Test
    void strongCorroborationApprovesRefund() {
        FraudAssessment a = processor.assess(facts(), evidenceOf(55));
        assertThat(a.riskScore()).isBetween(35, 69);
        assertThat(a.recommendedAction()).isEqualTo(RecommendedAction.APPROVE_DISPUTE_REFUND);
        assertThat(a.policyCitations()).isNotEmpty();
        assertThat(a.keyFactors()).isNotEmpty();
    }

    @Test
    void weakEvidenceDeniesDispute() {
        FraudAssessment a = processor.assess(facts(), evidenceOf(10));
        assertThat(a.riskScore()).isLessThan(35);
        assertThat(a.riskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(a.recommendedAction()).isEqualTo(RecommendedAction.DENY_DISPUTE);
    }

    @Test
    void serialDisputerWithWeakEvidenceGetsInfoRequest() {
        FraudAssessment a = processor.assess(facts(),
                evidenceOf(10, Map.of("priorDisputes", "5")));
        assertThat(a.recommendedAction())
                .isEqualTo(RecommendedAction.REQUEST_ADDITIONAL_INFORMATION);
    }

    @Test
    void mandatoryEscalationFlagsForceCriticalScore() {
        for (String flag : List.of("impossibleTravel", "cardTestingPatternDetected",
                "onInternalWatchlist", "accountTakeoverSignal")) {
            FraudAssessment a = processor.assess(facts(), evidenceOf(10, Map.of(flag, "true")));
            assertThat(a.riskScore()).as(flag).isGreaterThanOrEqualTo(70);
            assertThat(a.riskLevel()).as(flag).isEqualTo(RiskLevel.CRITICAL);
            assertThat(a.recommendedAction()).as(flag)
                    .isEqualTo(RecommendedAction.ESCALATE_TO_SENIOR_INVESTIGATOR);
        }
    }

    @Test
    void deterministicForSameInputs() {
        assertThat(processor.assess(facts(), evidenceOf(50)))
                .isEqualTo(processor.assess(facts(), evidenceOf(50)));
    }
}
