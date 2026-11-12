package com.fraudagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class FraudAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(FraudAgentApplication.class, args);
    }
}
