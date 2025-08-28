package com.nttdata.account_service.controller;

import com.nttdata.account_service.api.ApiApiController;
import com.nttdata.account_service.config.ApiExceptionHandler;
import com.nttdata.account_service.model.*;
import com.nttdata.account_service.service.AccountService;
import com.nttdata.account_service.service.impl.AccountApiDelegateImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;


public class AccountApiControllerWebTest {

    private WebTestClient webTestClient;
    private AccountService accountService;

    @BeforeEach
    void setUp() {
        accountService = mock(AccountService.class);
        ApiApiController controller = new ApiApiController(new AccountApiDelegateImpl(accountService));
        webTestClient = WebTestClient.bindToController(controller)
                .controllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void post_crearCuenta_debeRetornar201YLocation() {
        AccountResponse resp = new AccountResponse();
        resp.setId("A1");
        when(accountService.createAccount(any(AccountRequest.class))).thenReturn(Mono.just(resp));

        String body = "{ \"holderDocument\":\"12345678\", \"accountType\":\"CHECKING\" }";

        webTestClient.post().uri("/api/accounts")
                .bodyValue(body)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().valueEquals("Location", "/api/accounts/A1")
                .expectBody(AccountResponse.class)
                .isEqualTo(resp);
    }

    @Test
    void get_listarCuentas_debeRetornar200ConLista() {
        AccountResponse a = new AccountResponse();
        a.setId("1");
        AccountResponse b = new AccountResponse();
        b.setId("acc001");
        when(accountService.listAccounts()).thenReturn(Flux.just(a, b));

        webTestClient.get().uri("/api/accounts")
                .exchange()
                .expectStatus().isOk()
                .expectBody(InlineResponse200.class)
                .consumeWith(res -> {
                    InlineResponse200 body = res.getResponseBody();
                    assert body != null;
                    List<AccountResponse> items = body.getItems();
                    assert items.size() == 2;
                });
    }

    @Test
    void get_porId_debeRetornar200() {
        AccountResponse a = new AccountResponse();
        a.setId("acc001");
        when(accountService.getAccountById("acc001")).thenReturn(Mono.just(a));

        webTestClient.get().uri("/api/accounts/{id}", "acc001")
                .exchange()
                .expectStatus().isOk()
                .expectBody(AccountResponse.class)
                .isEqualTo(a);
    }

    @Test
    void put_actualizar_debeRetornar200() {
        AccountResponse updated = new AccountResponse();
        updated.setId("acc001");
        when(accountService.updateAccount(eq("acc001"), any(AccountRequest.class))).thenReturn(Mono.just(updated));

        String body = "{ \"holderDocument\":\"12345678\", \"accountType\":\"CHECKING\" }";

        webTestClient.put().uri("/api/accounts/{id}", "acc001")
                .bodyValue(body)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AccountResponse.class)
                .isEqualTo(updated);
    }

    @Test
    void delete_porId_debeRetornar204() {
        when(accountService.deleteAccount("acc001")).thenReturn(Mono.empty());

        webTestClient.delete().uri("/api/accounts/{id}", "acc001")
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void get_limits_porId_debeRetornar200() {
        AccountLimitsResponse limits = new AccountLimitsResponse();
        limits.setFreeTransactionsLimit(5);
        limits.setCommissionFee(new BigDecimal("1.50"));
        limits.setUsedTransactionsThisMonth(2);

        when(accountService.getAccountLimits("acc001")).thenReturn(Mono.just(limits));

        webTestClient.get().uri("/api/accounts/{id}/limits", "acc001")
                .exchange()
                .expectStatus().isOk()
                .expectBody(AccountLimitsResponse.class)
                .isEqualTo(limits);
    }

    @Test
    void get_porDocumentoTitular_debeRetornar404CuandoNoHayCuentas() {
        when(accountService.getAccountsByHolderDocument("12345678")).thenReturn(Flux.empty());

        webTestClient.get().uri("/api/accounts/holder/{document}", "12345678")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void post_balanceOps_debeRetornar200() {
        BalanceOperationResponse resp = new BalanceOperationResponse()
                .applied(true).newBalance(new BigDecimal("200")).commissionApplied(BigDecimal.ZERO).message("OK");

        when(accountService.applyBalanceOperation(eq("acc001"), any(BalanceOperationRequest.class)))
                .thenReturn(Mono.just(resp));


        webTestClient.post().uri("/api/accounts/{id}/balance-ops", "acc001")
                .bodyValue("{\"amount\":100,\"type\":\"deposit\",\"operationId\":\"op-1\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody(BalanceOperationResponse.class)
                .isEqualTo(resp);
    }
}
