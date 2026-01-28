package com.spring.batch.playground.jobs.csvprocessor.configuration;

import com.spring.batch.playground.jobs.csvprocessor.Transaction;
import com.spring.batch.playground.jobs.csvprocessor.TransactionCsv;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class TransactionImportJobConfiguration {

    @Bean
    public Step importTransactionsStep(
        JobRepository jobRepository,
        PlatformTransactionManager txManager,
        FlatFileItemReader<TransactionCsv> transactionReader,
        ItemProcessor<TransactionCsv, Transaction> transactionProcessor,
        JdbcBatchItemWriter<Transaction> transactionWriter
    ) {
        return new StepBuilder("importTransactionsStep", jobRepository)
            .<TransactionCsv, Transaction>chunk(1000)
            .transactionManager(txManager)
            .reader(transactionReader)
            .processor(transactionProcessor)
            .writer(transactionWriter)
            .faultTolerant()
            .skip(IllegalArgumentException.class)
            .skipLimit(50) // tolera linhas ruins até X
            .build();
    }

    @Bean
    public Job importTransactionsJob(
        org.springframework.batch.core.repository.JobRepository jobRepository,
        Step importTransactionsStep
    ) {
        return new org.springframework.batch.core.job.builder.JobBuilder("importTransactionsJob", jobRepository)
            .start(importTransactionsStep)
            .build();
    }

}
