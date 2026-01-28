package com.spring.batch.playground.jobs.addressenrichment;

public record CustomerRow(
    long id,
    String name,
    String zipcode
) {}
