package com.nttdata.account_service.integration.credits;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class CreditsClient {
    private final WebClient.Builder webClientBuilder;

    @Value("${services.credits.url}")
    private String baseUrl; // http://localhost:8585/api/v1

    public Mono<Boolean> hasActiveCreditCard(String customerId) {
        return webClientBuilder.baseUrl(baseUrl).build()
                .get()
                .uri(u -> u.path("/credits").queryParam("customerId", customerId).build())
                .retrieve()
                .bodyToFlux(CreditDTO.class)
                .filter(c -> "CREDIT_CARD".equals(c.getType()) && "ACTIVE".equals(c.getStatus()))
                .hasElements();
    }
}