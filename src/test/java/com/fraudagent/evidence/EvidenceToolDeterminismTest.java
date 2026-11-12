package com.fraudagent.evidence;

import com.fraudagent.domain.model.CaseFacts;
import com.fraudagent.domain.model.DisputeReason;
import com.fraudagent.domain.model.EvidenceItem;
import com.fraudagent.domain.port.EvidenceTool;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The six tools must be perfectly deterministic: identical case facts always
 * produce identical evidence, and different facts produce different evidence.
 */
class EvidenceToolDeterminismTest {

    private final List<EvidenceTool> tools = List.of(
            new TransactionVelocityTool(),
            new GeoAnomalyTool(),
            new DeviceReputationTool(),
            new MerchantRiskTool(),
            new CardTestingPatternTool(),
            new AccountHistoryTool());

    private CaseFacts facts(String txn, String card) {
        return new CaseFacts(1L, txn, card, "ACC-1", "MERCH-1", "ELECTRONICS",
                new BigDecimal("250.00"), "USD", Instant.parse("2026-11-01T10:00:00Z"),
                "US", "RO", "ECOMMERCE", DisputeReason.UNAUTHORIZED_TRANSACTION, "test");
    }

    @Test
    void sixToolsAreRegistered() {
        assertThat(tools).hasSize(6);
        assertThat(tools).extracting(EvidenceTool::toolName).doesNotHaveDuplicates();
    }

    @Test
    void identicalFactsProduceIdenticalEvidence() {
        for (EvidenceTool tool : tools) {
            EvidenceItem first = tool.run(facts("TXN-1", "card_a"));
            EvidenceItem second = tool.run(facts("TXN-1", "card_a"));
            assertThat(second).as(tool.toolName()).isEqualTo(first);
            assertThat(first.riskContribution()).as(tool.toolName()).isBetween(0, 100);
            assertThat(first.summary()).isNotBlank();
        }
    }

    @Test
    void differentFactsUsuallyProduceDifferentEvidence() {
        int differences = 0;
        for (EvidenceTool tool : tools) {
            if (!tool.run(facts("TXN-1", "card_a")).equals(tool.run(facts("TXN-2", "card_b")))) {
                differences++;
            }
        }
        assertThat(differences).isGreaterThanOrEqualTo(3);
    }

    @Test
    void crossBorderFactsRaiseGeoRisk() {
        GeoAnomalyTool geo = new GeoAnomalyTool();
        CaseFacts foreign = facts("TXN-X", "card_x");
        CaseFacts domestic = new CaseFacts(1L, "TXN-X", "card_x", "ACC-1", "MERCH-1",
                "ELECTRONICS", new BigDecimal("250.00"), "USD",
                Instant.parse("2026-11-01T10:00:00Z"), "US", "US", "CARD_PRESENT",
                DisputeReason.UNAUTHORIZED_TRANSACTION, "test");
        assertThat(geo.run(foreign).details().get("crossBorder")).isEqualTo("true");
        assertThat(geo.run(domestic).details().get("crossBorder")).isEqualTo("false");
    }
}
