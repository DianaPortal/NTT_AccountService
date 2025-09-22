package com.nttdata.accountservice.integration.credits;

import io.github.resilience4j.circuitbreaker.*;
import io.github.resilience4j.timelimiter.*;
import okhttp3.mockwebserver.*;
import org.junit.jupiter.api.*;
import org.springframework.test.util.*;
import org.springframework.web.reactive.function.client.*;
import reactor.test.*;

import static org.junit.Assert.*;

class CreditsClientTest {
  @Test
  void hasActiveCreditCard_trueCuandoExisteTarjetaActiva() throws Exception {
    try (MockWebServer server = new MockWebServer()) {
      server.enqueue(new MockResponse()
          .setResponseCode(200)
          .addHeader("Content-Type", "application/json")
          .setBody("[{\"id\":\"CR1\",\"type\":\"CREDIT_CARD\",\"status\":\"ACTIVE\"}]"));

      String base = server.url("/api/v1").toString();

      ExchangeFilterFunction noAuth = (request, next) -> next.exchange(request);
      WebClient.Builder builder = WebClient.builder().filter(noAuth);

      CreditsClient client = new CreditsClient(
          builder,
          CircuitBreakerRegistry.ofDefaults(),
          TimeLimiterRegistry.ofDefaults()
      );
      ReflectionTestUtils.setField(client, "baseUrl", base);

      StepVerifier.create(client.hasActiveCreditCard("CUST1"))
          .expectNext(true)
          .verifyComplete();

      RecordedRequest req = server.takeRequest();
      assertEquals("/api/v1/credits", req.getRequestUrl().encodedPath());
      assertEquals("CUST1", req.getRequestUrl().queryParameter("customerId"));
    }
  }
}
