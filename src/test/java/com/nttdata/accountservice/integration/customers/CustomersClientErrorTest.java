package com.nttdata.accountservice.integration.customers;

import io.github.resilience4j.circuitbreaker.*;
import io.github.resilience4j.timelimiter.*;
import okhttp3.mockwebserver.*;
import org.junit.jupiter.api.*;
import org.springframework.test.util.*;
import org.springframework.web.reactive.function.client.*;
import reactor.test.*;

class CustomersClientErrorTest {

  TimeLimiterRegistry relaxedRegistry = TimeLimiterRegistry.of(
      TimeLimiterConfig.custom()
          .timeoutDuration(java.time.Duration.ofSeconds(10))
          .build()
  );

  @Test
  void getEligibilityByDocument_500_lanzaError() throws Exception {
    try (MockWebServer server = new MockWebServer()) {
      server.enqueue(new MockResponse().setResponseCode(500).setBody("error"));
      String base = server.url("/api/v1").toString();

      ExchangeFilterFunction noAuth = (request, next) -> next.exchange(request);
      CustomersClient client = new CustomersClient(
          WebClient.builder(),
          CircuitBreakerRegistry.ofDefaults(),
          relaxedRegistry,
          noAuth
      );
      ReflectionTestUtils.setField(client, "baseUrl", base);

      StepVerifier.create(client.getEligibilityByDocument("DNI", "12345678"))
          .expectError(WebClientResponseException.class)
          .verify();
    }
  }
}
