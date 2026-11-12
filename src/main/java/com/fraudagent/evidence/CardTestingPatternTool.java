package com.fraudagent.evidence;

import com.fraudagent.domain.model.CaseFacts;
import com.fraudagent.domain.model.EvidenceItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * Tool 5 — card-testing pattern detection: bursts of small-value declined
 * authorisations immediately preceding the disputed charge, a signature of
 * criminals validating stolen card numbers.
 */
@Component
public class CardTestingPatternTool extends AbstractDeterministicTool {

    public static final String NAME = "card_testing_pattern";

    private static final BigDecimal CARD_TEST_THRESHOLD = new BigDecimal("2.00");

    @Override
    public String toolName() {
        return NAME;
    }

    @Override
    public String description() {
        return "Detects card-testing behaviour: small-value declined authorisations "
                + "and BIN-attack-style retries on the card in the hours before the "
                + "disputed transaction.";
    }

    @Override
    public EvidenceItem run(CaseFacts facts) {
        Random rng = seededRandom(facts);
        int declinedSmallAttempts = rng.nextInt(0, 15);
        int distinctMerchants = declinedSmallAttempts == 0 ? 0 : 1 + rng.nextInt(0, Math.min(6, declinedSmallAttempts));
        int windowMinutes = declinedSmallAttempts == 0 ? 0 : rng.nextInt(3, 240);
        boolean patternDetected = declinedSmallAttempts >= 4
                || (declinedSmallAttempts >= 2 && distinctMerchants >= 3);

        int risk = 0;
        risk += Math.min(50, declinedSmallAttempts * 7);
        if (distinctMerchants >= 3) {
            risk += 20;
        }
        if (windowMinutes > 0 && windowMinutes < 30) {
            risk += 15;
        }
        if (patternDetected) {
            risk += 15;
        }

        Map<String, String> details = new LinkedHashMap<>();
        details.put("declinedSmallValueAttempts", String.valueOf(declinedSmallAttempts));
        details.put("smallValueThreshold", CARD_TEST_THRESHOLD.toPlainString() + " " + facts.currency());
        details.put("distinctMerchantsTried", String.valueOf(distinctMerchants));
        details.put("attemptWindowMinutes", String.valueOf(windowMinutes));
        details.put("cardTestingPatternDetected", String.valueOf(patternDetected));

        String summary = patternDetected
                ? String.format("CARD TESTING PATTERN: %d small-value declines across %d merchants in %d minutes before the charge.",
                        declinedSmallAttempts, distinctMerchants, windowMinutes)
                : String.format("%d small-value declined attempt(s) before the charge; no card-testing pattern.",
                        declinedSmallAttempts);

        return new EvidenceItem(NAME, summary, clamp(risk), details);
    }
}
