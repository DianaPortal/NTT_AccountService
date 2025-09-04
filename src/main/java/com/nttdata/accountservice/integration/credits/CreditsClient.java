package com.nttdata.accountservice.integration.credits;

import io.github.resilience4j.circuitbreaker.*;
import io.github.resilience4j.reactor.circuitbreaker.operator.*;
import io.github.resilience4j.reactor.timelimiter.*;
import io.github.resilience4j.timelimiter.*;
import lombok.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.*;
import org.springframework.web.reactive.function.client.*;
import org.springframework.web.server.*;
import reactor.core.publisher.*;

import java.util.concurrent.*;

@Component
@RequiredArgsConstructor
public class CreditsClient {
  private final WebClient.Builder webClientBuilder;
  private final CircuitBreakerRegistry circuitBreakerRegistry;
  private final TimeLimiterRegistry timeLimiterRegistry;

  @Value("${services.credits.url}")
  private String baseUrl; // http://localhost:8585/api/v1

  public Mono<Boolean> hasActiveCreditCard(String customerId) {
    var cb = circuitBreakerRegistry.circuitBreaker("credits");
    var tl = timeLimiterRegistry.timeLimiter("credits");
    return webClientBuilder.baseUrl(baseUrl).build()
        .get()
        .uri(u -> u.path("/credits").queryParam("customerId", customerId).build())
        .retrieve()
        .bodyToFlux(CreditDTO.class)
        .transformDeferred(CircuitBreakerOperator.of(cb))
        .transformDeferred(TimeLimiterOperator.of(tl))
        .onErrorMap(TimeoutException.class,
            ex -> new ResponseStatusException(
                HttpStatus.GATEWAY_TIMEOUT, "Timeout en Credits (2s)", ex))
        .filter(c -> "CREDIT_CARD".equals(c.getType()) && "ACTIVE".equals(c.getStatus()))
        .hasElements();
  }
}