package com.fraudagent.evidence;

import com.fraudagent.domain.model.CaseFacts;
import com.fraudagent.domain.model.EvidenceItem;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * Tool 6 — account history and cardholder standing: account age, dispute
 * history, prior confirmed fraud and recent credential/contact changes that
 * may indicate account takeover.
 */
@Component
public class AccountHistoryTool extends AbstractDeterministicTool {

    public static final String NAME = "account_history_standing";

    @Override
    public String toolName() {
        return NAME;
    }

    @Override
    public String description() {
        return "Reviews the cardholder's account history: account age, number of "
                + "prior disputes and their outcomes, prior confirmed fraud, and "
                + "recent credential or contact-detail changes (account-takeover "
                + "indicators).";
    }

    @Override
    public EvidenceItem run(CaseFacts facts) {
        Random rng = seededRandom(facts);
        int accountAgeDays = 90 + rng.nextInt(0, 4000);
        int priorDisputes = rng.nextInt(0, 6);
        int priorConfirmedFraud = Math.min(priorDisputes, rng.nextInt(0, 3));
        boolean recentCredentialChange = rng.nextInt(0, 100) < 18;
        boolean recentContactChange = rng.nextInt(0, 100) < 12;

        int risk = 0;
        if (accountAgeDays < 180) {
            risk += 15;
        }
        if (priorDisputes >= 3) {
            risk += 15; // serial disputer — possible first-party fraud
        }
        if (recentCredentialChange) {
            risk += 25;
        }
        if (recentContactChange) {
            risk += 25;
        }

        Map<String, String> details = new LinkedHashMap<>();
        details.put("accountAgeDays", String.valueOf(accountAgeDays));
        details.put("priorDisputes", String.valueOf(priorDisputes));
        details.put("priorConfirmedFraud", String.valueOf(priorConfirmedFraud));
        details.put("credentialChangeLast30d", String.valueOf(recentCredentialChange));
        details.put("contactChangeLast30d", String.valueOf(recentContactChange));
        details.put("accountTakeoverSignal",
                String.valueOf(recentCredentialChange && recentContactChange));

        String summary = String.format(
                "Account %d days old, %d prior dispute(s) (%d confirmed fraud)%s%s.",
                accountAgeDays, priorDisputes, priorConfirmedFraud,
                recentCredentialChange ? ", credential changed <30d" : "",
                recentContactChange ? ", contact details changed <30d" : "");

        return new EvidenceItem(NAME, summary, clamp(risk), details);
    }
}
