package com.nttdata.accountservice.account.service;


import com.nttdata.accountservice.config.*;
import com.nttdata.accountservice.integration.credits.*;
import com.nttdata.accountservice.integration.customers.*;
import com.nttdata.accountservice.model.*;
import com.nttdata.accountservice.model.entity.*;
import com.nttdata.accountservice.repository.*;
import com.nttdata.accountservice.service.impl.*;
import com.nttdata.accountservice.service.policy.*;
import com.nttdata.accountservice.service.rules.*;
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
class AccountServiceImplApplyBalanceOperationTest {
  @Mock
  AccountRepository repository;
  @Mock
  CustomersClient customersClient;
  @Mock
  CreditsClient creditsClient;
  @Mock
  AccountRulesService rulesService;
  @Mock
  AccountPolicyService policyService;

  AccountServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new AccountServiceImpl(repository, customersClient, creditsClient, rulesService, policyService);
  }

  @Test
  void deposit_ok_aplicaSinErrores() {
    Account acc = new Account();
    acc.setId("A1");
    acc.setAccountType("SAVINGS");
    acc.setBalance(new BigDecimal("100.00"));
    acc.setFreeTransactionsLimit(99);           // sin comisión
    acc.setCommissionFee(new BigDecimal("1.50"));

    when(repository.findById("A1")).thenReturn(Mono.just(acc));
    when(repository.save(any(Account.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

    BalanceOperationRequest rq = new BalanceOperationRequest()
        .operationId("op-1")
        .type(BalanceOperationType.DEPOSIT)
        .amount(new BigDecimal("10"));

    StepVerifier.create(service.applyBalanceOperation("A1", rq))
        .assertNext(res -> {
          assertTrue(res.getApplied());
          assertEquals(0, new BigDecimal("110").compareTo(res.getNewBalance()));
          assertEquals(0, BigDecimal.ZERO.compareTo(res.getCommissionApplied()));
        })
        .verifyComplete();

    verify(repository).findById("A1");
    verify(repository).save(any(Account.class));
  }

  @Test
  void deposit_montoNegativo_rechazaOperacion() {
    BalanceOperationRequest rq = new BalanceOperationRequest()
        .operationId("op-err")
        .type(BalanceOperationType.DEPOSIT)
        .amount(new BigDecimal("-1"));

    StepVerifier.create(service.applyBalanceOperation("A1", rq))
        .expectErrorSatisfies(ex -> {
          assertInstanceOf(IllegalArgumentException.class, ex);
          assertTrue(ex.getMessage().toLowerCase().contains("amount"));
        })
        .verify();

    verifyNoInteractions(repository);
  }

  @Test
  void withdraw_conSaldoInsuficiente_lanzaError() {
    Account acc = new Account();
    acc.setId("A1");
    acc.setAccountType("SAVINGS");
    acc.setBalance(new BigDecimal("5"));        // saldo insuficiente
    acc.setFreeTransactionsLimit(0);
    acc.setCommissionFee(new BigDecimal("1.50"));

    when(repository.findById("A1")).thenReturn(Mono.just(acc));

    BalanceOperationRequest rq = new BalanceOperationRequest()
        .operationId("op-2")
        .type(BalanceOperationType.WITHDRAWAL)
        .amount(new BigDecimal("10"));

    StepVerifier.create(service.applyBalanceOperation("A1", rq))
        .expectErrorSatisfies(ex -> {
          // “Saldo insuficiente”
          assertInstanceOf(BusinessException.class, ex);
          assertTrue(ex.getMessage().contains("Saldo insuficiente"));
        })
        .verify();

    verify(repository).findById("A1");
    verify(repository, never()).save(any());
  }

  @Test
  void deposit_superaLimite_cobraComision() {
    Account acc = new Account();
    acc.setId("A2");
    acc.setAccountType("SAVINGS");
    acc.setBalance(new BigDecimal("100"));
    acc.setFreeTransactionsLimit(0);            // sin libres => cobra
    acc.setCommissionFee(new BigDecimal("1.50"));

    when(repository.findById("A2")).thenReturn(Mono.just(acc));
    when(repository.save(any(Account.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

    BalanceOperationRequest rq = new BalanceOperationRequest()
        .operationId("op-3")
        .type(BalanceOperationType.DEPOSIT)
        .amount(new BigDecimal("10"));

    StepVerifier.create(service.applyBalanceOperation("A2", rq))
        .assertNext(res -> {
          assertTrue(res.getApplied());
          assertEquals(0, new BigDecimal("110").compareTo(res.getNewBalance()));
          assertEquals(0, new BigDecimal("1.50").compareTo(res.getCommissionApplied()));
        })
        .verifyComplete();
  }

  @Test
  void applyBalanceOperation_idempotente_noDuplica() {
    Account acc = new Account();
    acc.setId("A3");
    acc.setAccountType("SAVINGS");
    acc.setBalance(new BigDecimal("100"));
    acc.setFreeTransactionsLimit(99);
    acc.setCommissionFee(new BigDecimal("1.50"));
    java.util.ArrayList<String> ids = new java.util.ArrayList<>();
    ids.add("op-dup");
    acc.setOpIds(ids);

    when(repository.findById("A3")).thenReturn(Mono.just(acc));

    BalanceOperationRequest rq = new BalanceOperationRequest()
        .operationId("op-dup")                // ya existe
        .type(BalanceOperationType.DEPOSIT)
        .amount(new BigDecimal("10"));

    StepVerifier.create(service.applyBalanceOperation("A3", rq))
        .assertNext(res -> {
          assertFalse(res.getApplied());      // idempotente
          assertEquals(0, new BigDecimal("100").compareTo(res.getNewBalance()));
        })
        .verifyComplete();
  }

  @Test
  void withdraw_fixedTerm_enDiaNoPermitido() {
    Account acc = new Account();
    acc.setId("A4");
    acc.setAccountType("FIXED_TERM");
    int notToday = java.time.LocalDate.now().getDayOfMonth() == 28 ? 27 : 28;
    acc.setAllowedDayOfMonth(notToday);           // garantizamos que NO sea hoy
    acc.setBalance(new BigDecimal("100"));
    acc.setFreeTransactionsLimit(99);
    acc.setCommissionFee(new BigDecimal("1.50"));

    when(repository.findById("A4")).thenReturn(Mono.just(acc));

    BalanceOperationRequest rq = new BalanceOperationRequest()
        .operationId("op-4")
        .type(BalanceOperationType.WITHDRAWAL)
        .amount(new BigDecimal("10"));

    StepVerifier.create(service.applyBalanceOperation("A4", rq))
        .expectErrorSatisfies(ex -> {
          assertInstanceOf(BusinessException.class, ex);
          assertTrue(ex.getMessage().contains("Día no permitido"));
        })
        .verify();
  }
}
