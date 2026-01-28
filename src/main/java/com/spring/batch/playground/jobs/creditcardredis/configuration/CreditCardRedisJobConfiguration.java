package com.spring.batch.playground.jobs.creditcardredis.configuration;

import com.spring.batch.playground.jobs.csvprocessor.TransactionCsv;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.infrastructure.item.support.SynchronizedItemStreamReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.batch.core.configuration.annotation.StepScope;

import java.util.HashMap;
import java.util.Map;

@Configuration
@Slf4j
public class CreditCardRedisJobConfiguration {

    private final ResourceLoader resourceLoader;
    @Value("${app.batch.credit-card.input-file:classpath:csv/credit_card_transactions.csv}")
    private String defaultInputFile;

    public CreditCardRedisJobConfiguration(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Bean
    public Job creditCardEurToRedisJob(JobRepository jobRepository, Step creditCardEurToRedisStep) {
        return new JobBuilder("creditCardEurToRedisJob", jobRepository)
            .start(creditCardEurToRedisStep)
            .build();
    }

    @Bean
    @StepScope
    public FlatFileItemReader<TransactionCsv> creditCardTransactionReader(
        @Value("#{jobParameters['inputFile']}") String inputFile
    ) {
        String resourcePath = (inputFile == null || inputFile.isBlank()) ? defaultInputFile : inputFile;
        Resource resource = resourceLoader.getResource(resourcePath);

        return new FlatFileItemReaderBuilder<TransactionCsv>()
            .name("creditCardTransactionReader")
            .resource(resource)
            .linesToSkip(1)
            .delimited()
            .names("externalId", "customerId", "amount", "currency", "type", "createdAt")
            .fieldSetMapper(fs -> new TransactionCsv(
                fs.readString("externalId"),
                fs.readString("customerId"),
                fs.readString("amount"),
                fs.readString("currency"),
                fs.readString("type"),
                fs.readString("createdAt")
            ))
            .build();
    }

    @Bean
    @StepScope
    public SynchronizedItemStreamReader<TransactionCsv> creditCardTransactionSynchronizedReader(
        FlatFileItemReader<TransactionCsv> creditCardTransactionReader
    ) {
        return new SynchronizedItemStreamReader<>(creditCardTransactionReader);
    }

    @Bean
    public ItemProcessor<TransactionCsv, TransactionCsv> creditCardEurFilterProcessor() {
        return item -> "EUR".equals(item.currency()) ? item : null;
    }

    @Bean
    public ItemWriter<TransactionCsv> creditCardRedisWriter(StringRedisTemplate redisTemplate) {
        return items -> {
            HashOperations<String, String, String> hashOps = redisTemplate.opsForHash();
            for (TransactionCsv item : items) {
                String key = "credit_card_tx:" + item.externalId();
                Map<String, String> fields = new HashMap<>();
                fields.put("externalId", item.externalId());
                fields.put("customerId", item.customerId());
                fields.put("amount", item.amount());
                fields.put("currency", item.currency());
                fields.put("type", item.type());
                fields.put("createdAt", item.createdAt());
                hashOps.putAll(key, fields);
                log.info("Inserted transaction into Redis: key={}", key);
            }
        };
    }

    @Bean
    public AsyncTaskExecutor creditCardTaskExecutor() {
        var executor = new SimpleAsyncTaskExecutor("credit-card-");
        executor.setConcurrencyLimit(5);
        return executor;
    }

    @Bean
    public Step creditCardEurToRedisStep(
        JobRepository jobRepository,
        PlatformTransactionManager txManager,
        SynchronizedItemStreamReader<TransactionCsv> creditCardTransactionSynchronizedReader,
        ItemProcessor<TransactionCsv, TransactionCsv> creditCardEurFilterProcessor,
        ItemWriter<TransactionCsv> creditCardRedisWriter,
        AsyncTaskExecutor creditCardTaskExecutor
    ) {
        return new StepBuilder("creditCardEurToRedisStep", jobRepository)
            .<TransactionCsv, TransactionCsv>chunk(500)
            .transactionManager(txManager)
            .reader(creditCardTransactionSynchronizedReader)
            .processor(creditCardEurFilterProcessor)
            .writer(creditCardRedisWriter)
            .taskExecutor(creditCardTaskExecutor)
            .build();
    }


}
