package com.spring.batch.playground.jobs.addressenrichment.viaCep;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ViaCepResponse(
    String logradouro,
    String bairro,
    String localidade,
    String uf,
    Boolean erro
) {}
