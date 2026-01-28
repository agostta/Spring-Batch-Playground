package com.spring.batch.playground.jobs.addressenrichment;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "external.address")
public record AddressApiProperties(
    String baseUrl,
    int timeoutMs,
    int minDelayMs
) {}
