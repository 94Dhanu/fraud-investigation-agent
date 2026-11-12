package com.fraudagent.evidence;

import com.fraudagent.domain.model.CaseFacts;
import com.fraudagent.domain.model.EvidenceItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * Tool 1 — analyses transaction velocity: how many authorisations this card
 * attempted in the trailing 1h / 24h windows and how far the disputed amount
 * deviates from the cardholder's 30-day average ticket.
 */
@Component
public class TransactionVelocityTool extends AbstractDeterministicTool {

    public static final String NAME = "transaction_velocity_check";

    @Override
    public String toolName() {
        return NAME;
    }

    @Override
    public String description() {
        return "Checks transaction velocity for the card: authorisations in the last "
                + "1 hour and 24 hours, and deviation of the disputed amount from "
                + "the cardholder's 30-day average ticket size.";
    }

    @Override
    public EvidenceItem run(CaseFacts facts) {
        Random rng = seededRandom(facts);
        int txnsLastHour = rng.nextInt(0, 12);
        int txnsLast24h = txnsLastHour + rng.nextInt(0, 30);
        BigDecimal avgTicket30d = BigDecimal.valueOf(10 + rng.nextInt(0, 400))
                .setScale(2, RoundingMode.HALF_UP);
        double deviation = avgTicket30d.signum() == 0 ? 0
                : facts.amount().divide(avgTicket30d, 4, RoundingMode.HALF_UP).doubleValue();

        int risk = 0;
        risk += Math.min(40, txnsLastHour * 8);
        risk += Math.min(20, txnsLast24h);
        if (deviation > 10) {
            risk += 30;
        } else if (deviation > 4) {
            risk += 15;
        }

        Map<String, String> details = new LinkedHashMap<>();
        details.put("txnsLastHour", String.valueOf(txnsLastHour));
        details.put("txnsLast24h", String.valueOf(txnsLast24h));
        details.put("avgTicket30d", avgTicket30d.toPlainString());
        details.put("amountVsAvgMultiple", String.format("%.2f", deviation));

        String summary = String.format(
                "%d authorisations in last hour, %d in last 24h; disputed amount is %.1fx the 30-day average ticket (%s).",
                txnsLastHour, txnsLast24h, deviation, avgTicket30d.toPlainString());

        return new EvidenceItem(NAME, summary, clamp(risk), details);
    }
}
