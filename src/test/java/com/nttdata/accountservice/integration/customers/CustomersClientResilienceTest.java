package com.nttdata.accountservice.integration.customers;

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

class CustomersClientResilienceTest {
  @Test
  void timeoutDe2s_dispara504() throws Exception {
    MockWebServer server = new MockWebServer();
    server.enqueue(new MockResponse().setBody("{}").setBodyDelay(3, TimeUnit.SECONDS)); // > 2s
    String base = server.url("/api/v1").toString();

    var cbReg = CircuitBreakerRegistry.ofDefaults();
    var tlReg = TimeLimiterRegistry.of(TimeLimiterConfig.custom().timeoutDuration(Duration.ofSeconds(2)).build());

    CustomersClient client = new CustomersClient(WebClient.builder(), cbReg, tlReg);
    ReflectionTestUtils.setField(client, "baseUrl", base);

    StepVerifier.create(client.getEligibilityByDocument("DNI", "12345678"))
        .expectError(ResponseStatusException.class) // 504
        .verify();

    server.shutdown();
  }
}
