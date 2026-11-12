package com.fraudagent.evidence;

import com.fraudagent.domain.model.CaseFacts;
import com.fraudagent.domain.model.EvidenceItem;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Tool 4 — merchant risk profile: merchant category risk band, historical
 * chargeback rate and internal watchlist membership.
 */
@Component
public class MerchantRiskTool extends AbstractDeterministicTool {

    public static final String NAME = "merchant_risk_profile";

    private static final Set<String> HIGH_RISK_MCC = Set.of(
            "GAMBLING", "CRYPTO", "MONEY_TRANSFER", "ADULT", "PHONE_CARDS", "DIGITAL_GOODS");

    @Override
    public String toolName() {
        return NAME;
    }

    @Override
    public String description() {
        return "Looks up the merchant's risk profile: merchant-category risk band, "
                + "90-day chargeback rate, acquirer risk tier and internal "
                + "watchlist membership.";
    }

    @Override
    public EvidenceItem run(CaseFacts facts) {
        Random rng = seededRandom(facts);
        boolean highRiskCategory = facts.merchantCategory() != null
                && HIGH_RISK_MCC.contains(facts.merchantCategory().toUpperCase());
        double chargebackRate = highRiskCategory
                ? 1.5 + rng.nextDouble(0, 8)
                : rng.nextDouble(0, 2.5);
        boolean onWatchlist = rng.nextInt(0, 100) < (highRiskCategory ? 35 : 5);
        String acquirerTier = onWatchlist ? "D" : (chargebackRate > 4 ? "C" : (chargebackRate > 1.5 ? "B" : "A"));

        int risk = 0;
        if (highRiskCategory) {
            risk += 25;
        }
        risk += (int) Math.min(40, chargebackRate * 6);
        if (onWatchlist) {
            risk += 35;
        }

        Map<String, String> details = new LinkedHashMap<>();
        details.put("merchantCategory", String.valueOf(facts.merchantCategory()));
        details.put("highRiskCategory", String.valueOf(highRiskCategory));
        details.put("chargebackRate90d", String.format("%.2f%%", chargebackRate));
        details.put("acquirerRiskTier", acquirerTier);
        details.put("onInternalWatchlist", String.valueOf(onWatchlist));

        String summary = String.format(
                "Merchant %s (%s): 90-day chargeback rate %.2f%%, acquirer tier %s%s.",
                facts.merchantId(), facts.merchantCategory(), chargebackRate, acquirerTier,
                onWatchlist ? ", ON INTERNAL WATCHLIST" : "");

        return new EvidenceItem(NAME, summary, clamp(risk), details);
    }
}
