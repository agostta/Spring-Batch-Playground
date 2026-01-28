package com.spring.batch.playground.jobs.csvprocessor;

public record TransactionCsv(
    String externalId,
    String customerId,
    String amount,
    String currency,
    String type,
    String createdAt
) {}