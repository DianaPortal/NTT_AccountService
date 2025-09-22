package com.nttdata.accountservice.integration.credits;

import io.github.resilience4j.circuitbreaker.*;
import io.github.resilience4j.timelimiter.*;
import okhttp3.mockwebserver.*;
import org.junit.jupiter.api.*;
import org.springframework.test.util.*;
import org.springframework.web.reactive.function.client.*;
import org.springframework.web.server.*;
import reactor.test.*;

import java.time.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class CreditsClientTimeoutTest {

  @Test
  void timeout_mapeaAGatewayTimeout_504() throws Exception {
    try (MockWebServer server = new MockWebServer()) {
      server.enqueue(new MockResponse()
          .setResponseCode(200)
          .setBody("[{\"id\":\"1\",\"customerId\":\"C1\",\"type\":\"CREDIT_CARD\",\"status\":\"ACTIVE\"}]")
          .setBodyDelay(500, TimeUnit.MILLISECONDS));
      String base = server.url("/api/v1").toString();

      TimeLimiterConfig cfg = TimeLimiterConfig.custom()
          .timeoutDuration(Duration.ofMillis(50))
          .build();
      TimeLimiterRegistry tlr = TimeLimiterRegistry.of(cfg);

      ExchangeFilterFunction noAuth = (request, next) -> next.exchange(request);
      WebClient.Builder builder = WebClient.builder().filter(noAuth);

      CreditsClient client = new CreditsClient(
          builder,
          CircuitBreakerRegistry.ofDefaults(),
          tlr
      );
      ReflectionTestUtils.setField(client, "baseUrl", base);

      StepVerifier.create(client.hasActiveCreditCard("C1"))
          .expectErrorSatisfies(ex -> {
            assertInstanceOf(ResponseStatusException.class, ex);
            assertEquals(504, ((ResponseStatusException) ex).getStatus().value());
          })
          .verify();
    }
  }
}
