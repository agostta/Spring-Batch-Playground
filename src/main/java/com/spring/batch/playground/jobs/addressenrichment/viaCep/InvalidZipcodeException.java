package com.spring.batch.playground.jobs.addressenrichment.viaCep;

public class InvalidZipcodeException extends RuntimeException {
    public InvalidZipcodeException(String message) {
        super(message);
    }
}
