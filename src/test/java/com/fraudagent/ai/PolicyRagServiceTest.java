package com.fraudagent.ai;

import com.fraudagent.domain.model.PolicyCitation;
import com.fraudagent.evidence.AccountHistoryTool;
import com.fraudagent.evidence.CardTestingPatternTool;
import com.fraudagent.evidence.DeviceReputationTool;
import com.fraudagent.evidence.GeoAnomalyTool;
import com.fraudagent.evidence.MerchantRiskTool;
import com.fraudagent.evidence.TransactionVelocityTool;
import com.fraudagent.domain.model.CaseFacts;
import com.fraudagent.domain.model.DisputeReason;
import com.fraudagent.domain.model.EvidenceItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.SimpleVectorStore;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyRagServiceTest {

    private PolicyRagService ragService;

    @BeforeEach
    void setUp() throws Exception {
        ragService = new PolicyRagService(
                SimpleVectorStore.builder(new LocalHashingEmbeddingModel()).build(), 4);
        ragService.indexPolicyCorpus();
    }

    @Test
    void corpusIsIndexedWithExactCitations() {
        assertThat(ragService.corpusIndex()).hasSizeGreaterThanOrEqualTo(15);
        assertThat(ragService.corpusIndex())
                .extracting(PolicyCitation::documentId)
                .contains("POL-100", "POL-202", "POL-305", "POL-410", "POL-515", "POL-620");
    }

    @Test
    void impossibleTravelQueryRetrievesGeoPolicy() {
        List<PolicyCitation> hits = ragService.retrieve("impossible travel cloned card", 3);
        assertThat(hits).isNotEmpty();
        assertThat(hits.get(0).documentId()).isEqualTo("POL-305");
        assertThat(hits.get(0).excerpt()).containsIgnoringCase("impossible");
    }

    @Test
    void velocityQueryRetrievesVelocityPolicy() {
        List<PolicyCitation> hits = ragService.retrieve("velocity limits card testing declined attempts", 3);
        assertThat(hits).isNotEmpty();
        assertThat(hits.get(0).documentId()).isEqualTo("POL-202");
    }

    @Test
    void caseDrivenRetrievalProducesRelevantCitations() {
        CaseFacts facts = new CaseFacts(1L, "TXN-1", "card_fp_x1", "ACC-1", "MERCH-1",
                "CRYPTO", new BigDecimal("500"), "USD", Instant.now().minusSeconds(3600),
                "US", "NG", "ECOMMERCE", DisputeReason.UNAUTHORIZED_TRANSACTION, "n");
        List<EvidenceItem> evidence = List.of(
                new TransactionVelocityTool(), new GeoAnomalyTool(), new DeviceReputationTool(),
                new MerchantRiskTool(), new CardTestingPatternTool(), new AccountHistoryTool())
                .stream().map(t -> t.run(facts)).toList();

        List<PolicyCitation> citations = ragService.retrieveForCase(facts, evidence);
        assertThat(citations).isNotEmpty();
        assertThat(citations).allSatisfy(c -> {
            assertThat(c.documentId()).startsWith("POL-");
            assertThat(c.excerpt()).isNotBlank();
            assertThat(c.section()).isNotBlank();
        });
    }
}
