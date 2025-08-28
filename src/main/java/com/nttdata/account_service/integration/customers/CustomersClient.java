package com.nttdata.account_service.integration.customers;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class CustomersClient {
    private final WebClient.Builder webClientBuilder;

    @Value("${services.customers.url}")
    private String baseUrl; // http://localhost:8086/api/v1

    public Mono<EligibilityResponse> getEligibilityByDocument(String documentNumber) {
        return webClientBuilder.baseUrl(baseUrl).build()
                .get()
                .uri(u -> u.path("/customers/eligibility")
                        .queryParam("documentNumber", documentNumber)
                        .build())
                .retrieve()
                .bodyToMono(EligibilityResponse.class);
    }
}