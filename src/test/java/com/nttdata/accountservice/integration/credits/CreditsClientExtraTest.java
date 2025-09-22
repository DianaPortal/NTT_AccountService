package com.nttdata.accountservice.integration.credits;

import io.github.resilience4j.circuitbreaker.*;
import io.github.resilience4j.timelimiter.*;
import okhttp3.mockwebserver.*;
import org.junit.jupiter.api.*;
import org.springframework.test.util.*;
import org.springframework.web.reactive.function.client.*;
import reactor.test.*;

import java.time.*;

class CreditsClientExtraTest {

  TimeLimiterRegistry relaxedRegistry = TimeLimiterRegistry.of(
      TimeLimiterConfig.custom()
          .timeoutDuration(Duration.ofSeconds(10))
          .build()
  );

  @Test
  void hasActiveCreditCard_listaVacia_devuelveFalse() throws Exception {
    try (MockWebServer server = new MockWebServer()) {
      server.enqueue(new MockResponse()
          .setResponseCode(200)
          .addHeader("Content-Type", "application/json")
          .setBody("[]"));
      String base = server.url("/api/v1").toString();

      ExchangeFilterFunction noAuth = (request, next) -> next.exchange(request);
      WebClient.Builder builder = WebClient.builder().filter(noAuth);

      CreditsClient client = new CreditsClient(
          builder,
          CircuitBreakerRegistry.ofDefaults(),
          relaxedRegistry
      );
      ReflectionTestUtils.setField(client, "baseUrl", base);

      StepVerifier.create(client.hasActiveCreditCard("C1"))
          .expectNext(false)
          .verifyComplete();
    }
  }

  @Test
  void hasActiveCreditCard_500_lanzaError() throws Exception {
    try (MockWebServer server = new MockWebServer()) {
      server.enqueue(new MockResponse().setResponseCode(500).setBody("error"));
      String base = server.url("/api/v1").toString();

      ExchangeFilterFunction noAuth = (request, next) -> next.exchange(request);
      WebClient.Builder builder = WebClient.builder().filter(noAuth);

      CreditsClient client = new CreditsClient(
          builder,
          CircuitBreakerRegistry.ofDefaults(),
          relaxedRegistry
      );
      ReflectionTestUtils.setField(client, "baseUrl", base);

      StepVerifier.create(client.hasActiveCreditCard("C1"))
          .expectError(WebClientResponseException.class)
          .verify();
    }
  }
}
