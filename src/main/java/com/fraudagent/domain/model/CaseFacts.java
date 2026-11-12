package com.fraudagent.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Framework-neutral, immutable view of the facts gathered at case intake.
 * Passed to evidence tools and processors so the domain never depends on
 * JPA entities or Spring types.
 */
public record CaseFacts(
        Long caseId,
        String transactionId,
        String cardFingerprint,
        String accountId,
        String merchantId,
        String merchantCategory,
        BigDecimal amount,
        String currency,
        Instant occurredAt,
        String customerCountry,
        String merchantCountry,
        String channel,
        DisputeReason disputeReason,
        String customerNarrative
) {
}
