package com.nttdata.account_service.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

public class ApiExceptionHandlerTest {

    WebTestClient client;

    @RestController
    static class BoomController {
        @GetMapping("/bad") Mono<Void> bad() { return Mono.error(new IllegalArgumentException("x")); }
        @GetMapping("/conflict") Mono<Void> conflict() { return Mono.error(new DuplicateKeyException("dup")); }
        @GetMapping("/biz") Mono<Void> biz() { return Mono.error(new BusinessException("regla")); }
        @GetMapping("/status") Mono<Void> status() { return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "nope")); }
        @GetMapping("/oops") Mono<Void> oops() { return Mono.error(new RuntimeException("boom")); }
    }

    @BeforeEach
    void setup() {
        client = WebTestClient.bindToController(new BoomController())
                .controllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test void badRequest_400() { client.get().uri("/bad").exchange().expectStatus().isBadRequest(); }

    @Test void conflict_409() { client.get().uri("/conflict").exchange().expectStatus().isEqualTo(HttpStatus.CONFLICT); }

    @Test void business_422() { client.get().uri("/biz").exchange().expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY); }

    @Test void statusException_passthrough() { client.get().uri("/status").exchange().expectStatus().isNotFound(); }

    @Test void internal_500() { client.get().uri("/oops").exchange().expectStatus().is5xxServerError(); }
}
