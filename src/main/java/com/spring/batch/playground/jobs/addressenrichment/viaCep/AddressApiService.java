package com.spring.batch.playground.jobs.addressenrichment.viaCep;

import java.time.Duration;

import com.spring.batch.playground.jobs.addressenrichment.AddressApiProperties;
import com.spring.batch.playground.jobs.addressenrichment.ExternalServiceTemporaryException;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
public class AddressApiService {

    private final WebClient webClient;
    private final AddressApiProperties props;

    public AddressApiService(WebClient webClient, AddressApiProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    public ViaCepResponse fetchAddressByZipcode(String zipcode) {
        try {
            if (props.minDelayMs() > 0) {
                Thread.sleep(props.minDelayMs());
            }

            return webClient.get()
                .uri("/ws/{zipcode}/json/", zipcode)
                .retrieve()
                .bodyToMono(ViaCepResponse.class)
                .timeout(Duration.ofMillis(props.timeoutMs()))
                .block();

        } catch (WebClientResponseException e) {
            if (e.getStatusCode().is5xxServerError()) {
                throw new ExternalServiceTemporaryException("ViaCEP 5xx", e);
            }
            throw new InvalidZipcodeException("HTTP " + e.getStatusCode() + " for zipcode=" + zipcode);

        } catch (InvalidZipcodeException | ExternalServiceTemporaryException e) {
            throw e;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExternalServiceTemporaryException("Interrupted while rate limiting", e);

        } catch (Exception e) {
            throw new ExternalServiceTemporaryException("Error calling ViaCEP for zipcode=" + zipcode, e);
        }
    }
}
