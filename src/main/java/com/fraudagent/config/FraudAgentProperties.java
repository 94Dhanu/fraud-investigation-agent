package com.fraudagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fraud.agent")
public record FraudAgentProperties(
        String mode,
        RiskThresholds riskThresholds
) {
    public record RiskThresholds(
            int denyBelow,
            int escalateAtOrAbove
    ) {
    }
}
