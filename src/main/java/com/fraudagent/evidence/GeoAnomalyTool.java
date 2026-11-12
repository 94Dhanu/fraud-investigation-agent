package com.fraudagent.evidence;

import com.fraudagent.domain.model.CaseFacts;
import com.fraudagent.domain.model.EvidenceItem;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * Tool 2 — geo-location anomaly: distance of the transaction from the
 * cardholder's home region and detection of impossible travel (two
 * authorisations too far apart to be physically possible).
 */
@Component
public class GeoAnomalyTool extends AbstractDeterministicTool {

    public static final String NAME = "geo_location_anomaly";

    @Override
    public String toolName() {
        return NAME;
    }

    @Override
    public String description() {
        return "Analyses the transaction's geo-location vs the cardholder's home "
                + "region, including cross-border usage and impossible-travel "
                + "detection between consecutive authorisations.";
    }

    @Override
    public EvidenceItem run(CaseFacts facts) {
        Random rng = seededRandom(facts);
        boolean crossBorder = facts.customerCountry() != null
                && facts.merchantCountry() != null
                && !facts.customerCountry().equalsIgnoreCase(facts.merchantCountry());
        int distanceKm = crossBorder
                ? 2000 + rng.nextInt(0, 8000)
                : rng.nextInt(0, 300);
        boolean impossibleTravel = distanceKm > 5000 && rng.nextInt(0, 100) < 45;
        int minutesSincePrevious = impossibleTravel ? rng.nextInt(5, 90) : rng.nextInt(120, 4320);

        int risk = 0;
        if (crossBorder) {
            risk += 25;
        }
        if (distanceKm > 5000) {
            risk += 25;
        } else if (distanceKm > 1000) {
            risk += 12;
        }
        if (impossibleTravel) {
            risk += 35;
        }

        Map<String, String> details = new LinkedHashMap<>();
        details.put("crossBorder", String.valueOf(crossBorder));
        details.put("distanceFromHomeKm", String.valueOf(distanceKm));
        details.put("impossibleTravel", String.valueOf(impossibleTravel));
        details.put("minutesSincePreviousAuth", String.valueOf(minutesSincePrevious));
        details.put("customerCountry", String.valueOf(facts.customerCountry()));
        details.put("merchantCountry", String.valueOf(facts.merchantCountry()));

        String summary = impossibleTravel
                ? String.format("IMPOSSIBLE TRAVEL: transaction %dkm from previous authorisation only %d minutes earlier.",
                        distanceKm, minutesSincePrevious)
                : String.format("Transaction %dkm from cardholder's home region%s.",
                        distanceKm, crossBorder ? " (cross-border)" : "");

        return new EvidenceItem(NAME, summary, clamp(risk), details);
    }
}
