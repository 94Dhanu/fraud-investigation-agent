package com.fraudagent.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FraudCaseApiTest {

    @Autowired
    private MockMvc mvc;

    private static final String VALID_BODY = """
            {
              "transactionId": "TXN-API-1",
              "cardFingerprint": "card_fp_api001",
              "accountId": "ACC-900",
              "merchantId": "MERCH-77",
              "merchantCategory": "CRYPTO",
              "amount": 512.50,
              "currency": "USD",
              "occurredAt": "2026-11-10T08:30:00Z",
              "customerCountry": "US",
              "merchantCountry": "NG",
              "channel": "ECOMMERCE",
              "disputeReason": "UNAUTHORIZED_TRANSACTION",
              "customerNarrative": "I did not make this purchase."
            }
            """;

    @Test
    void intakeInvestigateAndReviewOverRest() throws Exception {
        String json = mvc.perform(post("/api/cases")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fraudCase.status").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.evidence", hasSize(6)))
                .andExpect(jsonPath("$.latestAssessment.recommendedAction").isString())
                .andExpect(jsonPath("$.latestAssessment.policyCitations", not(empty())))
                .andExpect(jsonPath("$.auditTrail", hasSize(greaterThanOrEqualTo(4))))
                .andReturn().getResponse().getContentAsString();

        long caseId = new ObjectMapper().readTree(json).path("fraudCase").path("id").asLong();

        mvc.perform(get("/api/cases/" + caseId)).andExpect(status().isOk());

        mvc.perform(post("/api/review/" + caseId + "/decision")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"reviewerId":"analyst.api","decision":"APPROVE_RECOMMENDATION",
                         "justification":"evidence supports it"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("APPROVE_RECOMMENDATION"));

        mvc.perform(get("/api/cases/" + caseId))
                .andExpect(jsonPath("$.fraudCase.status").value("RESOLVED"));
    }

    @Test
    void invalidIntakeReturns400WithDetails() throws Exception {
        mvc.perform(post("/api/cases")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"transactionId": "", "amount": -5, "currency": "usd"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details", not(empty())));
    }

    @Test
    void policySearchReturnsExactCitations() throws Exception {
        mvc.perform(get("/api/policies/search").param("q", "impossible travel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].documentId").value("POL-305"))
                .andExpect(jsonPath("$[0].excerpt").isString());
    }

    @Test
    void missingCaseReturns404() throws Exception {
        mvc.perform(get("/api/cases/999999")).andExpect(status().isNotFound());
    }
}
