package com.nttdata.accountservice.account.service;

/*
 * Ramas adicionales de applyBalanceOperation.
 * Incluye: operationId vacío, cuenta no encontrada, TRANSFER_OUT, TRANSFER_IN con comisión,
 * operación COMMISSION, retiro exitoso, FIXED_TERM día permitido,
 * FIXED_TERM saldo insuficiente.
 */

import com.nttdata.accountservice.config.BusinessException;
import com.nttdata.accountservice.integration.credits.CreditsClient;
import com.nttdata.accountservice.integration.customers.CustomersClient;
import com.nttdata.accountservice.model.BalanceOperationRequest;
import com.nttdata.accountservice.model.BalanceOperationType;
import com.nttdata.accountservice.repository.AccountRepository;
import com.nttdata.accountservice.service.impl.AccountServiceImpl;
import com.nttdata.accountservice.service.policy.AccountPolicyService;
import com.nttdata.accountservice.service.rules.AccountRulesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class AccountServiceImplBalanceAdditionalBranchesTest {

  @Mock AccountRepository repository;
  @Mock CustomersClient customersClient;
  @Mock CreditsClient creditsClient;
  @Mock AccountRulesService rulesService;
  @Mock AccountPolicyService policyService;

  AccountServiceImpl service;

  @BeforeEach
  void init() {
    service = new AccountServiceImpl(repository, customersClient, creditsClient, rulesService, policyService);
  }

  @Test
  void applyBalanceOperation_operationIdVacio_error() {
    BalanceOperationRequest rq = new BalanceOperationRequest()
        .operationId("")
        .type(BalanceOperationType.DEPOSIT)
        .amount(new BigDecimal("10"));

    StepVerifier.create(service.applyBalanceOperation("X", rq))
        .expectErrorSatisfies(ex -> {
          assertInstanceOf(IllegalArgumentException.class, ex);
          assertTrue(ex.getMessage().contains("operationId"));
        })
        .verify();
    verifyNoInteractions(repository);
  }

  @Test
  void applyBalanceOperation_accountNoExiste_notFound() {
    when(repository.findById("NOPE")).thenReturn(Mono.empty());
    BalanceOperationRequest rq = new BalanceOperationRequest()
        .operationId("op")
        .type(BalanceOperationType.DEPOSIT)
        .amount(new BigDecimal("5"));

    StepVerifier.create(service.applyBalanceOperation("NOPE", rq))
        .expectErrorSatisfies(ex -> assertInstanceOf(ResponseStatusException.class, ex))
        .verify();
  }

  @Test
  void transferOut_exitoso_debito() {
    com.nttdata.accountservice.model.entity.Account acc = new com.nttdata.accountservice.model.entity.Account();
    acc.setId("A1");
    acc.setAccountType("SAVINGS");
    acc.setBalance(new BigDecimal("100"));
    acc.setFreeTransactionsLimit(99);
    acc.setCommissionFee(new BigDecimal("1.00"));
    when(repository.findById("A1")).thenReturn(Mono.just(acc));
    when(repository.save(any(com.nttdata.accountservice.model.entity.Account.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

    BalanceOperationRequest rq = new BalanceOperationRequest()
        .operationId("to-1")
        .type(BalanceOperationType.TRANSFER_OUT)
        .amount(new BigDecimal("25"));

    StepVerifier.create(service.applyBalanceOperation("A1", rq))
        .assertNext(res -> {
          assertTrue(res.getApplied());
          assertEquals(0, new BigDecimal("75").compareTo(res.getNewBalance()));
        })
        .verifyComplete();
  }

  @Test
  void transferIn_exitoso_credito_conComisionCuandoSuperaLimite() {
    com.nttdata.accountservice.model.entity.Account acc = new com.nttdata.accountservice.model.entity.Account();
    acc.setId("A2");
    acc.setAccountType("SAVINGS");
    acc.setBalance(new BigDecimal("50"));
    acc.setFreeTransactionsLimit(0); // cualquier operación genera comisión
    acc.setCommissionFee(new BigDecimal("2.00"));
    when(repository.findById("A2")).thenReturn(Mono.just(acc));
    when(repository.save(any(com.nttdata.accountservice.model.entity.Account.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

    BalanceOperationRequest rq = new BalanceOperationRequest()
        .operationId("ti-1")
        .type(BalanceOperationType.TRANSFER_IN)
        .amount(new BigDecimal("40"));

    StepVerifier.create(service.applyBalanceOperation("A2", rq))
        .assertNext(res -> {
          assertTrue(res.getApplied());
          assertEquals(0, new BigDecimal("90").compareTo(res.getNewBalance()));
          assertEquals(0, new BigDecimal("2.00").compareTo(res.getCommissionApplied()));
        })
        .verifyComplete();
  }

  @Test
  void commissionType_noCuentaParaPolitica_deltaCero() {
    com.nttdata.accountservice.model.entity.Account acc = new com.nttdata.accountservice.model.entity.Account();
    acc.setId("A3");
    acc.setAccountType("SAVINGS");
    acc.setBalance(new BigDecimal("100"));
    // Si contara aplicaría comisión, pero no debe contar
    acc.setFreeTransactionsLimit(0);
    acc.setCommissionFee(new BigDecimal("1.00"));
    when(repository.findById("A3")).thenReturn(Mono.just(acc));
    when(repository.save(any(com.nttdata.accountservice.model.entity.Account.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

    BalanceOperationRequest rq = new BalanceOperationRequest()
        .operationId("c-1")
        // no debe afectar balance ni contador
        .type(BalanceOperationType.COMMISSION)
        .amount(new BigDecimal("999"));

    StepVerifier.create(service.applyBalanceOperation("A3", rq))
        .assertNext(res -> {
          assertTrue(res.getApplied());
          assertEquals(0, new BigDecimal("100").compareTo(res.getNewBalance()));
          assertEquals(0, BigDecimal.ZERO.compareTo(res.getCommissionApplied()));
        })
        .verifyComplete();
  }

  @Test
  void withdrawal_exitoso_conSaldoSuficiente() {
    com.nttdata.accountservice.model.entity.Account acc = new com.nttdata.accountservice.model.entity.Account();
    acc.setId("A4");
    acc.setAccountType("SAVINGS");
    acc.setBalance(new BigDecimal("200"));
    acc.setFreeTransactionsLimit(10);
    acc.setCommissionFee(new BigDecimal("1.00"));
    when(repository.findById("A4")).thenReturn(Mono.just(acc));
    when(repository.save(any(com.nttdata.accountservice.model.entity.Account.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

    BalanceOperationRequest rq = new BalanceOperationRequest()
        .operationId("wd-1")
        .type(BalanceOperationType.WITHDRAWAL)
        .amount(new BigDecimal("60"));

    StepVerifier.create(service.applyBalanceOperation("A4", rq))
        .assertNext(res -> {
          assertTrue(res.getApplied());
          assertEquals(0, new BigDecimal("140").compareTo(res.getNewBalance()));
        })
        .verifyComplete();
  }

  @Test
  void fixedTerm_withdrawal_enDiaPermitido_ok() {
    com.nttdata.accountservice.model.entity.Account acc = new com.nttdata.accountservice.model.entity.Account();
    acc.setId("A5");
    acc.setAccountType("FIXED_TERM");
    acc.setAllowedDayOfMonth(LocalDate.now().getDayOfMonth()); // hoy permitido
    acc.setBalance(new BigDecimal("80"));
    acc.setFreeTransactionsLimit(5);
    acc.setCommissionFee(new BigDecimal("1"));
    when(repository.findById("A5")).thenReturn(Mono.just(acc));
    when(repository.save(any(com.nttdata.accountservice.model.entity.Account.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

    BalanceOperationRequest rq = new BalanceOperationRequest()
        .operationId("fx-1")
        .type(BalanceOperationType.WITHDRAWAL)
        .amount(new BigDecimal("30"));

    StepVerifier.create(service.applyBalanceOperation("A5", rq))
        .assertNext(res -> {
          assertTrue(res.getApplied());
          assertEquals(0, new BigDecimal("50").compareTo(res.getNewBalance()));
        })
        .verifyComplete();
  }

  @Test
  void withdrawal_fixedTerm_diaPermitido_peroSaldoInsuficiente() {
    com.nttdata.accountservice.model.entity.Account acc = new com.nttdata.accountservice.model.entity.Account();
    acc.setId("A6");
    acc.setAccountType("FIXED_TERM");
    acc.setAllowedDayOfMonth(LocalDate.now().getDayOfMonth());
    acc.setBalance(new BigDecimal("10"));
    acc.setFreeTransactionsLimit(9);
    acc.setCommissionFee(new BigDecimal("1"));
    when(repository.findById("A6")).thenReturn(Mono.just(acc));

    BalanceOperationRequest rq = new BalanceOperationRequest()
        .operationId("fx-2")
        .type(BalanceOperationType.WITHDRAWAL)
        .amount(new BigDecimal("50"));

    StepVerifier.create(service.applyBalanceOperation("A6", rq))
        .expectErrorSatisfies(ex -> {
          assertInstanceOf(BusinessException.class, ex);
          assertTrue(ex.getMessage().contains("Saldo insuficiente"));
        })
        .verify();
  }

  @Test
  void fixedTerm_withdrawal_diaNoPermitido_error() {
    com.nttdata.accountservice.model.entity.Account acc = new com.nttdata.accountservice.model.entity.Account();
    acc.setId("A7");
    acc.setAccountType("FIXED_TERM");
    // forzar distinto al de hoy (si coincide, +1 mod)
    acc.setAllowedDayOfMonth((LocalDate.now().getDayOfMonth() % 28) + 1);
    acc.setBalance(new BigDecimal("100"));
    acc.setFreeTransactionsLimit(5);
    acc.setCommissionFee(new BigDecimal("1"));
    when(repository.findById("A7")).thenReturn(Mono.just(acc));

    BalanceOperationRequest rq = new BalanceOperationRequest()
        .operationId("fx-err")
        .type(BalanceOperationType.WITHDRAWAL)
        .amount(new BigDecimal("10"));

    StepVerifier.create(service.applyBalanceOperation("A7", rq))
        .expectErrorSatisfies(ex -> {
          assertInstanceOf(BusinessException.class, ex);
          assertTrue(ex.getMessage().contains("Día no permitido"));
        })
        .verify();
  }

  @Test
  void fixedTerm_deposit_diaNoPermitido_sePermite() {
    com.nttdata.accountservice.model.entity.Account acc = new com.nttdata.accountservice.model.entity.Account();
    acc.setId("A8");
    acc.setAccountType("FIXED_TERM");
    acc.setAllowedDayOfMonth((LocalDate.now().getDayOfMonth() % 28) + 1); // distinto hoy
    acc.setBalance(new BigDecimal("20"));
    acc.setFreeTransactionsLimit(10);
    acc.setCommissionFee(new BigDecimal("1"));
    when(repository.findById("A8")).thenReturn(Mono.just(acc));
    when(repository.save(any(com.nttdata.accountservice.model.entity.Account.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

    BalanceOperationRequest rq = new BalanceOperationRequest()
        .operationId("fx-dep")
        .type(BalanceOperationType.DEPOSIT)
        .amount(new BigDecimal("30"));

    StepVerifier.create(service.applyBalanceOperation("A8", rq))
        .assertNext(res -> {
          assertTrue(res.getApplied());
          assertEquals(0, new BigDecimal("50").compareTo(res.getNewBalance()));
        })
        .verifyComplete();
  }

  @Test
  void deposit_reutilizaOpsCounterMesActual_sinReset() {
    com.nttdata.accountservice.model.entity.Account acc = new com.nttdata.accountservice.model.entity.Account();
    acc.setId("A9");
    acc.setAccountType("SAVINGS");
    acc.setBalance(new BigDecimal("10"));
    acc.setFreeTransactionsLimit(100); // evitar comisión
    acc.setCommissionFee(new BigDecimal("5"));
    com.nttdata.accountservice.model.entity.OpsCounter oc = new com.nttdata.accountservice.model.entity.OpsCounter();
    oc.setYearMonth(java.time.YearMonth.now().toString());
    oc.setCount(5);
    acc.setOpsCounter(oc);
    when(repository.findById("A9")).thenReturn(Mono.just(acc));
    when(repository.save(any(com.nttdata.accountservice.model.entity.Account.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

    BalanceOperationRequest rq = new BalanceOperationRequest()
        .operationId("dep-1")
        .type(BalanceOperationType.DEPOSIT)
        .amount(new BigDecimal("15"));

    StepVerifier.create(service.applyBalanceOperation("A9", rq))
        .assertNext(res -> {
          assertTrue(res.getApplied());
          assertEquals(0, new BigDecimal("25").compareTo(res.getNewBalance()));
        })
        .verifyComplete();

    // Verificar que el contador se incrementó a 6 y no se reseteó a 0
    assertEquals(6, acc.getOpsCounter().getCount());
  }

  @Test
  void deposit_conOpIdsAlLimite_trimPrimero() {
    com.nttdata.accountservice.model.entity.Account acc = new com.nttdata.accountservice.model.entity.Account();
    acc.setId("A10");
    acc.setAccountType("SAVINGS");
    acc.setBalance(new BigDecimal("100"));
    acc.setFreeTransactionsLimit(1000);
    acc.setCommissionFee(new BigDecimal("1"));
    // lista de 200 ids
    ArrayList<String> ids = new ArrayList<>();
    for (int i = 0; i < 200; i++) ids.add("op-" + i);
    acc.setOpIds(ids);
    when(repository.findById("A10")).thenReturn(Mono.just(acc));
    when(repository.save(any(com.nttdata.accountservice.model.entity.Account.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

    BalanceOperationRequest rq = new BalanceOperationRequest()
        .operationId("op-200")
        .type(BalanceOperationType.DEPOSIT)
        .amount(new BigDecimal("5"));

    StepVerifier.create(service.applyBalanceOperation("A10", rq))
        .expectNextCount(1)
        .verifyComplete();

    assertEquals(200, acc.getOpIds().size());
    assertFalse(acc.getOpIds().contains("op-0"), "Debe haberse eliminado el primero");
    assertTrue(acc.getOpIds().contains("op-200"));
  }
}
