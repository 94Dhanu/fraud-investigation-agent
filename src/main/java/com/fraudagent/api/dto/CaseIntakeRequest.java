package com.fraudagent.api.dto;

import com.fraudagent.domain.model.DisputeReason;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Fraud dispute case intake payload (REST + dashboard form).
 */
public record CaseIntakeRequest(

        @NotBlank(message = "transactionId is required")
        @Pattern(regexp = "^[A-Za-z0-9_-]{4,64}$",
                message = "transactionId must be 4-64 alphanumeric/dash characters")
        String transactionId,

        @NotBlank(message = "cardFingerprint is required")
        @Size(min = 8, max = 64, message = "cardFingerprint must be 8-64 characters")
        String cardFingerprint,

        @NotBlank(message = "accountId is required")
        @Size(min = 4, max = 64)
        String accountId,

        @NotBlank(message = "merchantId is required")
        @Size(min = 3, max = 64)
        String merchantId,

        @NotBlank(message = "merchantCategory is required")
        @Size(max = 64)
        String merchantCategory,

        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be greater than zero")
        BigDecimal amount,

        @NotBlank
        @Pattern(regexp = "^[A-Z]{3}$", message = "currency must be a 3-letter ISO code")
        String currency,

        @NotNull(message = "occurredAt is required")
        Instant occurredAt,

        @NotBlank
        @Pattern(regexp = "^[A-Z]{2}$", message = "customerCountry must be a 2-letter ISO code")
        String customerCountry,

        @NotBlank
        @Pattern(regexp = "^[A-Z]{2}$", message = "merchantCountry must be a 2-letter ISO code")
        String merchantCountry,

        @NotBlank(message = "channel is required")
        @Pattern(regexp = "^(CARD_PRESENT|ECOMMERCE|MOTO|ATM|RECURRING)$",
                message = "channel must be CARD_PRESENT, ECOMMERCE, MOTO, ATM or RECURRING")
        String channel,

        @NotNull(message = "disputeReason is required")
        DisputeReason disputeReason,

        @Size(max = 2000)
        String customerNarrative
) {
}
