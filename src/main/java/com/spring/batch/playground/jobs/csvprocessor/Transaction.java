package com.spring.batch.playground.jobs.csvprocessor;

public record Transaction(
    String externalId,
    String customerId,
    long amountInCents,
    String currency,
    long feeInCents,
    String type,
    java.time.Instant createdAt
) {}
