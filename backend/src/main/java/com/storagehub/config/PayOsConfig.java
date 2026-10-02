package com.storagehub.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.payos.PayOS;

@Configuration
@EnableConfigurationProperties(PayOsProperties.class)
public class PayOsConfig {

    @Bean
    public PayOS payOS(PayOsProperties properties) {
        String clientId = properties.clientId() != null ? properties.clientId() : "mock-client-id";
        String apiKey = properties.apiKey() != null ? properties.apiKey() : "mock-api-key";
        String checksumKey = properties.checksumKey() != null ? properties.checksumKey() : "mock-checksum-key";
        return new PayOS(clientId, apiKey, checksumKey);
    }
}
