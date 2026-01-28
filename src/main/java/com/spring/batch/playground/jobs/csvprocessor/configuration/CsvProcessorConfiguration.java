package com.spring.batch.playground.jobs.csvprocessor.configuration;

import javax.sql.DataSource;

import com.spring.batch.playground.jobs.csvprocessor.Transaction;
import com.spring.batch.playground.jobs.csvprocessor.TransactionCsv;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

@Configuration
@EnableConfigurationProperties(BatchInputProperties.class)
public class CsvProcessorConfiguration {

    private final ResourceLoader resourceLoader;
    private final String defaultInputFile;

    public CsvProcessorConfiguration(
        ResourceLoader resourceLoader,
        BatchInputProperties batchInputProperties
    ) {
        this.resourceLoader = resourceLoader;
        this.defaultInputFile = batchInputProperties.getInputFile();
    }

    @Bean
    @StepScope
    public FlatFileItemReader<TransactionCsv> transactionReader(
        @Value("#{jobParameters['inputFile']}") String inputFile
    ) {
        String resourcePath = (inputFile == null || inputFile.isBlank()) ? defaultInputFile : inputFile;
        Resource resource = resourceLoader.getResource(resourcePath);

        return new FlatFileItemReaderBuilder<TransactionCsv>()
            .name("transactionReader")
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
    public ItemProcessor<TransactionCsv, Transaction> transactionProcessor() {
        return item -> {
            var amount = new java.math.BigDecimal(item.amount());

            if (amount.compareTo(java.math.BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("amount must be > 0");
            }

            var currency = item.currency();
            if (!currency.equals("BRL") && !currency.equals("USD")) {
                throw new IllegalArgumentException("unsupported currency: " + currency);
            }

            var type = item.type();
            var rate = switch (type) {
                case "PURCHASE" -> new java.math.BigDecimal("0.025");
                case "REFUND" -> new java.math.BigDecimal("0.010");
                default -> throw new IllegalArgumentException("unsupported type: " + type);
            };

            long amountInCents = amount.movePointRight(2)
                .setScale(0, java.math.RoundingMode.HALF_UP)
                .longValueExact();
            long feeInCents = amount.multiply(rate)
                .movePointRight(2)
                .setScale(0, java.math.RoundingMode.HALF_UP)
                .longValueExact();

            var createdAt = java.time.Instant.parse(item.createdAt() + "Z");

            return new Transaction(
                item.externalId(),
                item.customerId(),
                amountInCents,
                currency,
                feeInCents,
                type,
                createdAt
            );
        };
    }

    @Bean
    public JdbcBatchItemWriter<Transaction> transactionWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<Transaction>()
            .dataSource(dataSource)
            .sql("""
                insert into transactions
                (external_id, customer_id, amount_in_cents, currency, fee_in_cents, transaction_type, created_at)
                values (:externalId, :customerId, :amountInCents, :currency, :feeInCents, :type, :createdAt)
                """)
            .itemSqlParameterSourceProvider(this::transactionSqlParameterSource)
            .build();
    }

    private SqlParameterSource transactionSqlParameterSource(Transaction transaction) {
        return new MapSqlParameterSource()
            .addValue("externalId", transaction.externalId())
            .addValue("customerId", transaction.customerId())
            .addValue("amountInCents", transaction.amountInCents())
            .addValue("currency", transaction.currency())
            .addValue("feeInCents", transaction.feeInCents())
            .addValue("type", transaction.type())
            .addValue(
                "createdAt",
                java.time.OffsetDateTime.ofInstant(transaction.createdAt(), java.time.ZoneOffset.UTC),
                java.sql.Types.TIMESTAMP_WITH_TIMEZONE
            );
    }
}
