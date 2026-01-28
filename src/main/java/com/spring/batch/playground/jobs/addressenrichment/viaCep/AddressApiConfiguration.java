package com.spring.batch.playground.jobs.addressenrichment.viaCep;

import com.spring.batch.playground.jobs.addressenrichment.AddressApiProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(AddressApiProperties.class)
public class AddressApiConfiguration {

    @Bean
    public WebClient addressWebClient(AddressApiProperties props) {
        return WebClient.builder()
            .baseUrl(props.baseUrl())
            .build();
    }
}
