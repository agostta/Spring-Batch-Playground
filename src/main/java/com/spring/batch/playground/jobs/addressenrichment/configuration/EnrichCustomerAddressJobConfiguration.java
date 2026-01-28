package com.spring.batch.playground.jobs.addressenrichment.configuration;

import java.util.Map;
import javax.sql.DataSource;

import com.spring.batch.playground.jobs.addressenrichment.viaCep.AddressApiService;
import com.spring.batch.playground.jobs.addressenrichment.CustomerAddressUpdate;
import com.spring.batch.playground.jobs.addressenrichment.CustomerRow;
import com.spring.batch.playground.jobs.addressenrichment.ExternalServiceTemporaryException;
import com.spring.batch.playground.jobs.addressenrichment.viaCep.InvalidZipcodeException;
import com.spring.batch.playground.jobs.addressenrichment.viaCep.ViaCepResponse;
import lombok.SneakyThrows;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.database.JdbcPagingItemReader;
import org.springframework.batch.infrastructure.item.database.Order;
import org.springframework.batch.infrastructure.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.infrastructure.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.infrastructure.item.database.support.PostgresPagingQueryProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class EnrichCustomerAddressJobConfiguration {

    @Bean
    @SneakyThrows
    public JdbcPagingItemReader<CustomerRow> customerPendingReader(DataSource dataSource) {
        var queryProvider = new PostgresPagingQueryProvider();
        queryProvider.setSelectClause("select id, name, zipcode");
        queryProvider.setFromClause("from customers");
        queryProvider.setWhereClause("where address_status = 'PENDING'");
        queryProvider.setSortKeys(Map.of("id", Order.ASCENDING));

        return new JdbcPagingItemReaderBuilder<CustomerRow>()
            .name("customerPendingReader")
            .dataSource(dataSource)
            .queryProvider(queryProvider)
            .pageSize(200)
            .rowMapper((rs, rowNum) -> new CustomerRow(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("zipcode")
            ))
            .build();
    }

    @Bean
    public ItemProcessor<CustomerRow, CustomerAddressUpdate> enrichAddressProcessor(AddressApiService api) {
        return item -> {
            String zipcode = normalizeZipcode(item.zipcode());

            if (zipcode.length() != 8 || !zipcode.chars().allMatch(Character::isDigit)) {
                throw new InvalidZipcodeException("Invalid zipcode format: " + item.zipcode());
            }

            ViaCepResponse response = api.fetchAddressByZipcode(zipcode);
            if (response == null) {
                throw new ExternalServiceTemporaryException("Null response from provider", null);
            }

            if (Boolean.TRUE.equals(response.erro())) {
                return new CustomerAddressUpdate(
                    item.id(),
                    null,
                    null,
                    null,
                    null,
                    "INVALID_ZIPCODE"
                );
            }

            return new CustomerAddressUpdate(
                item.id(),
                response.logradouro(),
                response.bairro(),
                response.localidade(),
                response.uf(),
                "ENRICHED"
            );
        };
    }

    @Bean
    public JdbcBatchItemWriter<CustomerAddressUpdate> customerAddressWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<CustomerAddressUpdate>()
            .dataSource(dataSource)
            .sql("""
                update customers
                   set street = :street,
                       neighborhood = :neighborhood,
                       city = :city,
                       state = :state,
                       address_status = :addressStatus,
                       updated_at = now()
                 where id = :id
                """)
            .beanMapped()
            .build();
    }

    @Bean
    public Step enrichCustomerAddressStep(
        JobRepository jobRepository,
        PlatformTransactionManager txManager,
        JdbcPagingItemReader<CustomerRow> customerPendingReader,
        ItemProcessor<CustomerRow, CustomerAddressUpdate> enrichAddressProcessor,
        JdbcBatchItemWriter<CustomerAddressUpdate> customerAddressWriter
    ) {
        return new StepBuilder("enrichCustomerAddressStep", jobRepository)
            .<CustomerRow, CustomerAddressUpdate>chunk(100)
            .transactionManager(txManager)
            .reader(customerPendingReader)
            .processor(enrichAddressProcessor)
            .writer(customerAddressWriter)
            .faultTolerant()
            .retry(ExternalServiceTemporaryException.class)
            .retryLimit(3)
            .skip(InvalidZipcodeException.class)
            .skipLimit(10_000)
            .build();
    }

    @Bean
    public Job enrichCustomerAddressJob(
        JobRepository jobRepository,
        Step enrichCustomerAddressStep
    ) {
        return new JobBuilder("enrichCustomerAddressJob", jobRepository)
            .start(enrichCustomerAddressStep)
            .build();
    }

    private static String normalizeZipcode(String zipcode) {
        if (zipcode == null) {
            return "";
        }
        return zipcode.replaceAll("[^0-9]", "");
    }
}
