package com.fraudagent.api.web;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Template helpers: consistent UTC timestamps and enum prettifying.
 */
@Component("fmt")
public class FormatSupport {

    private static final DateTimeFormatter TS = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm 'UTC'", Locale.ROOT)
            .withZone(ZoneOffset.UTC);

    public String instant(Instant value) {
        return value == null ? "—" : TS.format(value);
    }

    public String enumLabel(Enum<?> value) {
        if (value == null) {
            return "—";
        }
        String[] parts = value.name().toLowerCase(Locale.ROOT).split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(' ');
            }
        }
        return sb.toString().trim();
    }

    public String money(BigDecimal amount, String currency) {
        return amount == null ? "—" : amount.toPlainString() + " " + (currency == null ? "" : currency);
    }

    public String statusBadge(String status) {
        if (status == null) {
            return "bg-secondary";
        }
        return switch (status) {
            case "PENDING_REVIEW" -> "bg-warning text-dark";
            case "RESOLVED" -> "bg-success";
            case "INVESTIGATING" -> "bg-info text-dark";
            case "MORE_INFO_REQUESTED" -> "bg-primary";
            case "REJECTED" -> "bg-danger";
            default -> "bg-secondary";
        };
    }

    public String riskBadge(int score) {
        if (score >= 70) {
            return "bg-danger";
        }
        if (score >= 55) {
            return "bg-warning text-dark";
        }
        if (score >= 35) {
            return "bg-info text-dark";
        }
        return "bg-success";
    }

    public String riskBarClass(int score) {
        if (score >= 70) {
            return "bg-danger";
        }
        if (score >= 55) {
            return "bg-warning";
        }
        if (score >= 35) {
            return "bg-info";
        }
        return "bg-success";
    }
}
