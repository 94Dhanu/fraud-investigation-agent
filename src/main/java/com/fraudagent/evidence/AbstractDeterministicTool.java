package com.fraudagent.evidence;

import com.fraudagent.domain.model.CaseFacts;
import com.fraudagent.domain.port.EvidenceTool;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

/**
 * Base class for the synthetic evidence tools. Each tool derives a
 * deterministic {@link Random} stream from the SHA-256 of the case facts
 * plus the tool name, so identical facts always yield identical evidence.
 */
public abstract class AbstractDeterministicTool implements EvidenceTool {

    protected Random seededRandom(CaseFacts facts) {
        return new Random(seed(facts));
    }

    protected long seed(CaseFacts facts) {
        String material = String.join("|",
                toolName(),
                nullToEmpty(facts.transactionId()),
                nullToEmpty(facts.cardFingerprint()),
                nullToEmpty(facts.accountId()),
                nullToEmpty(facts.merchantId()),
                nullToEmpty(facts.merchantCategory()),
                String.valueOf(facts.amount()),
                nullToEmpty(facts.currency()),
                String.valueOf(facts.occurredAt()),
                nullToEmpty(facts.customerCountry()),
                nullToEmpty(facts.merchantCountry()),
                nullToEmpty(facts.channel()),
                facts.disputeReason() == null ? "" : facts.disputeReason().name());
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(material.getBytes(StandardCharsets.UTF_8));
            long seed = 0;
            for (int i = 0; i < 8; i++) {
                seed = (seed << 8) | (hash[i] & 0xFFL);
            }
            return seed;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    protected int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
