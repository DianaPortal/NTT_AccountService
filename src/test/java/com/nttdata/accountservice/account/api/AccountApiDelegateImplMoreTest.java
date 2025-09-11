package com.nttdata.accountservice.account.api;

import com.nttdata.accountservice.api.*;
import com.nttdata.accountservice.model.*;
import com.nttdata.accountservice.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.*;
import org.mockito.junit.jupiter.*;
import reactor.core.publisher.*;
import reactor.test.*;

import java.math.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class AccountApiDelegateImplMoreTest {
  @Mock
  AccountService service;
  AccountApiDelegateImpl delegate;

  @BeforeEach
  void setUp() {
    delegate = new AccountApiDelegateImpl(service);
  }

  @Test
  void getById_ok() {
    when(service.getAccountById("A1"))
        .thenReturn(Mono.just(new AccountResponse().id("A1")));

    StepVerifier.create(delegate.getAccountById("A1", null))
        .assertNext(re -> {
          assertEquals(200, re.getStatusCodeValue());
          assertNotNull(re.getBody());
          assertEquals("A1", re.getBody().getId());
        })
        .verifyComplete();
  }

  @Test
  void listAccounts_ok() {
    when(service.listAccounts())
        .thenReturn(Flux.just(new AccountResponse().id("A1")));


    StepVerifier.create(delegate.listAccounts(null))
        .assertNext(re -> {
          assertEquals(200, re.getStatusCodeValue());
          assertNotNull(re.getBody());
        })
        .verifyComplete();
  }

  @Test
  void updateAccount_ok() {
    AccountRequest rq = new AccountRequest().monthlyMovementLimit(8);
    when(service.updateAccount(eq("A1"), any(AccountRequest.class)))
        .thenReturn(Mono.just(new AccountResponse().id("A1").monthlyMovementLimit(8)));

    StepVerifier.create(delegate.updateAccount("A1", Mono.just(rq), null))
        .assertNext(re -> {
          assertEquals(200, re.getStatusCodeValue());
          assertNotNull(re.getBody());
          assertNotNull(re.getBody().getMonthlyMovementLimit());
          assertTrue(re.getBody().getMonthlyMovementLimit().isPresent());
          assertEquals(8, re.getBody().getMonthlyMovementLimit().get().intValue());
        })
        .verifyComplete();
  }

  @Test
  void deleteAccount_ok() {
    when(service.deleteAccount("A1")).thenReturn(Mono.empty());

    StepVerifier.create(delegate.deleteAccount("A1", null))
        .assertNext(re -> assertEquals(204, re.getStatusCodeValue()))
        .verifyComplete();
  }

  @Test
  void getAccountLimits_ok() {
    when(service.getAccountLimits("A1"))
        .thenReturn(Mono.just(new AccountLimitsResponse().freeTransactionsLimit(5)));

    StepVerifier.create(delegate.getAccountLimits("A1", null))
        .assertNext(re -> {
          assertEquals(200, re.getStatusCodeValue());
          assertNotNull(re.getBody());
          assertEquals(5, re.getBody().getFreeTransactionsLimit());
        })
        .verifyComplete();
  }

  @Test
  void applyBalanceOperation_ok() {
    BalanceOperationRequest rq = new BalanceOperationRequest()
        .operationId("op1")
        // .type(BalanceOperationRequest.TypeEnum.DEPOSIT)
        .type(BalanceOperationType.DEPOSIT)
        .amount(new BigDecimal("10"));

    when(service.applyBalanceOperation(eq("A1"), any(BalanceOperationRequest.class)))
        .thenReturn(Mono.just(new BalanceOperationResponse().applied(true)));

    StepVerifier.create(delegate.applyBalanceOperation("A1", Mono.just(rq), null))
        .assertNext(re -> {
          assertEquals(200, re.getStatusCodeValue());
          assertNotNull(re.getBody());
          assertTrue(re.getBody().getApplied());
        })
        .verifyComplete();
  }
}
