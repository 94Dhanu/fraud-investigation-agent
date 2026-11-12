package com.fraudagent.config;

import com.fraudagent.api.dto.CaseIntakeRequest;
import com.fraudagent.domain.model.DisputeReason;
import com.fraudagent.persistence.repository.FraudCaseRepository;
import com.fraudagent.service.FraudCaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Seeds a few realistic dispute cases through the real intake pipeline so the
 * dashboard is populated on first run. Disable with
 * {@code fraud.seed-demo-data=false}.
 */
@Component
@ConditionalOnProperty(name = "fraud.seed-demo-data", havingValue = "true", matchIfMissing = true)
public class DemoDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final FraudCaseRepository caseRepository;
    private final FraudCaseService caseService;

    public DemoDataSeeder(FraudCaseRepository caseRepository, FraudCaseService caseService) {
        this.caseRepository = caseRepository;
        this.caseService = caseService;
    }

    @Override
    public void run(String... args) {
        if (caseRepository.count() > 0) {
            return;
        }
        List<CaseIntakeRequest> seeds = List.of(
                new CaseIntakeRequest("TXN-DEMO-001", "card_fp_a19f88c2", "ACC-100231",
                        "MERCH-8810", "ELECTRONICS", new BigDecimal("1299.00"), "USD",
                        Instant.now().minus(26, ChronoUnit.HOURS), "US", "RO", "ECOMMERCE",
                        DisputeReason.UNAUTHORIZED_TRANSACTION,
                        "Cardholder states they never shopped at this merchant and were asleep at the time."),
                new CaseIntakeRequest("TXN-DEMO-002", "card_fp_b77e01d4", "ACC-100887",
                        "MERCH-1177", "GAMBLING", new BigDecimal("450.00"), "USD",
                        Instant.now().minus(9, ChronoUnit.HOURS), "US", "MT", "ECOMMERCE",
                        DisputeReason.CARD_NOT_PRESENT_FRAUD,
                        "Online gambling charge; cardholder claims card data was stolen."),
                new CaseIntakeRequest("TXN-DEMO-003", "card_fp_c33d92aa", "ACC-100455",
                        "MERCH-0042", "GROCERY", new BigDecimal("86.40"), "USD",
                        Instant.now().minus(52, ChronoUnit.HOURS), "US", "US", "CARD_PRESENT",
                        DisputeReason.FIRST_PARTY_FRAUD_CLAIM,
                        "Cardholder denies a charge at their regular supermarket."),
                new CaseIntakeRequest("TXN-DEMO-004", "card_fp_d55f17b9", "ACC-100998",
                        "ATM-NET-7", "ATM", new BigDecimal("800.00"), "USD",
                        Instant.now().minus(5, ChronoUnit.HOURS), "GB", "GB", "ATM",
                        DisputeReason.CARD_LOST_OR_STOLEN,
                        "Card reported stolen; two ATM withdrawals followed the theft report."));
        for (CaseIntakeRequest seed : seeds) {
            try {
                var created = caseService.intakeCase(seed);
                log.info("Seeded demo case #{} ({})", created.getId(), created.getTransactionId());
            } catch (Exception e) {
                log.warn("Failed to seed {}: {}", seed.transactionId(), e.getMessage());
            }
        }
    }
}
