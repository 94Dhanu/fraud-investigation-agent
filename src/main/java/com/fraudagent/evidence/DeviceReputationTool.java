package com.fraudagent.evidence;

import com.fraudagent.domain.model.CaseFacts;
import com.fraudagent.domain.model.EvidenceItem;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * Tool 3 — device fingerprint reputation: whether the transaction originated
 * from a device the cardholder has used before, device age, and prior fraud
 * reports linked to the device.
 */
@Component
public class DeviceReputationTool extends AbstractDeterministicTool {

    public static final String NAME = "device_fingerprint_reputation";

    @Override
    public String toolName() {
        return NAME;
    }

    @Override
    public String description() {
        return "Evaluates the device fingerprint behind the transaction: known vs "
                + "new device, device age in days, emulator/proxy indicators and "
                + "prior fraud reports associated with the device.";
    }

    @Override
    public EvidenceItem run(CaseFacts facts) {
        Random rng = seededRandom(facts);
        boolean knownDevice = rng.nextInt(0, 100) < 55;
        int deviceAgeDays = knownDevice ? 30 + rng.nextInt(0, 700) : rng.nextInt(0, 14);
        int priorFraudReports = knownDevice ? 0 : rng.nextInt(0, 4);
        boolean emulatorOrProxy = !knownDevice && rng.nextInt(0, 100) < 30;

        int risk = 0;
        if (!knownDevice) {
            risk += 30;
        }
        if (deviceAgeDays < 3) {
            risk += 20;
        }
        risk += Math.min(30, priorFraudReports * 12);
        if (emulatorOrProxy) {
            risk += 20;
        }

        Map<String, String> details = new LinkedHashMap<>();
        details.put("knownDevice", String.valueOf(knownDevice));
        details.put("deviceAgeDays", String.valueOf(deviceAgeDays));
        details.put("priorFraudReports", String.valueOf(priorFraudReports));
        details.put("emulatorOrProxyDetected", String.valueOf(emulatorOrProxy));

        String summary = knownDevice
                ? String.format("Recognised device (%d days old, no prior fraud reports).", deviceAgeDays)
                : String.format("UNKNOWN device (%d days old), %d prior fraud report(s)%s.",
                        deviceAgeDays, priorFraudReports,
                        emulatorOrProxy ? ", emulator/proxy indicators present" : "");

        return new EvidenceItem(NAME, summary, clamp(risk), details);
    }
}
