package com.spring.batch.playground.jobs.addressenrichment;

public class ExternalServiceTemporaryException extends RuntimeException {
    public ExternalServiceTemporaryException(String message, Throwable cause) {
        super(message, cause);
    }
}
