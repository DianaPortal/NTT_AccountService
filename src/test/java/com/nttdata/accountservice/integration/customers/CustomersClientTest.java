package com.nttdata.accountservice.integration.customers;

import io.github.resilience4j.circuitbreaker.*;
import io.github.resilience4j.timelimiter.*;
import okhttp3.mockwebserver.*;
import org.junit.jupiter.api.*;
import org.springframework.test.util.*;
import org.springframework.web.reactive.function.client.*;
import reactor.test.*;

import static org.junit.Assert.*;

public class CustomersClientTest {
  @Test
  void getEligibilityByDocument_enviaTipoYNumeroComoQueryParams() throws Exception {
    MockWebServer server = new MockWebServer();
    server.enqueue(new MockResponse()
        .setResponseCode(200)
        .addHeader("Content-Type", "application/json")
        .setBody("{\"customerId\":\"C1\",\"type\":\"PERSONAL\",\"profile\":\"VIP\",\"hasActiveCreditCard\":true}")
    );

    String base = server.url("/api/v1").toString();

    CustomersClient client = new CustomersClient(
        WebClient.builder(),
        CircuitBreakerRegistry.ofDefaults(),
        TimeLimiterRegistry.ofDefaults()
    );
    ReflectionTestUtils.setField(client, "baseUrl", base);

    StepVerifier.create(client.getEligibilityByDocument("DNI", "12345678"))
        .assertNext(r -> assertEquals("C1", r.getCustomerId()))
        .verifyComplete();

    RecordedRequest req = server.takeRequest();
    assertEquals("/api/v1/customers/eligibility", req.getRequestUrl().encodedPath());
    assertEquals("DNI", req.getRequestUrl().queryParameter("documentType"));
    assertEquals("12345678", req.getRequestUrl().queryParameter("documentNumber"));
    server.shutdown();
  }
}
