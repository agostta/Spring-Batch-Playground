package com.spring.batch.playground.jobs.addressenrichment;

public record CustomerAddressUpdate(
    long id,
    String street,
    String neighborhood,
    String city,
    String state,
    String addressStatus
) {}
