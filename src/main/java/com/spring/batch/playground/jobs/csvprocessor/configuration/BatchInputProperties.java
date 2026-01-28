package com.spring.batch.playground.jobs.csvprocessor.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.batch")
@Getter
@Setter
public class BatchInputProperties {

    private String inputFile = "classpath:csv/cash_transactions.csv";
}
