package com.nttdata.accountservice.integration.customers;

import io.github.resilience4j.circuitbreaker.*;
import io.github.resilience4j.reactor.circuitbreaker.operator.*;
import io.github.resilience4j.reactor.timelimiter.*;
import io.github.resilience4j.timelimiter.*;
import lombok.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.*;
import org.springframework.web.reactive.function.client.*;
import org.springframework.web.server.*;
import reactor.core.publisher.*;

import java.util.concurrent.*;

@Component
@RequiredArgsConstructor
public class CustomersClient {

  private final WebClient.Builder webClientBuilder;
  private final CircuitBreakerRegistry circuitBreakerRegistry;
  private final TimeLimiterRegistry timeLimiterRegistry;
  private final ExchangeFilterFunction bearerRelayFilter;

  @Value("${services.customers.url}")
  private String baseUrl; // http://localhost:8086/api/v1

  public Mono<EligibilityResponse> getEligibilityByDocument(
      String documentType, String documentNumber) {
    CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("customers");

    return webClientBuilder.baseUrl(baseUrl)
        .filter(bearerRelayFilter)
        .build()
        .get()
        .uri(uriBuilder -> uriBuilder
            .path("/customers/eligibility")
            .queryParam("documentType", documentType)
            .queryParam("documentNumber", documentNumber)
            .build())
        .retrieve()
        .bodyToMono(EligibilityResponse.class)
        .transformDeferred(CircuitBreakerOperator.of(cb))
        .transformDeferred(TimeLimiterOperator.of(timeLimiterRegistry.timeLimiter("customers")))
        .onErrorMap(TimeoutException.class,
            ex -> new ResponseStatusException(
                HttpStatus.GATEWAY_TIMEOUT, "Timeout en Customers (2s)", ex));
  }

}