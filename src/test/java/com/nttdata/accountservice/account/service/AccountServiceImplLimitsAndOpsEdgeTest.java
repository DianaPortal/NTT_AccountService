package com.nttdata.accountservice.account.service;

import com.nttdata.accountservice.integration.credits.CreditsClient;
import com.nttdata.accountservice.integration.customers.CustomersClient;
import com.nttdata.accountservice.model.BalanceOperationRequest;
import com.nttdata.accountservice.model.BalanceOperationResponse;
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
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplLimitsAndOpsEdgeTest {
  @Mock AccountRepository repository;
  @Mock CustomersClient customersClient;
  @Mock CreditsClient creditsClient;
  @Mock AccountRulesService rulesService;
  @Mock AccountPolicyService policyService;

  AccountServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new AccountServiceImpl(repository, customersClient, creditsClient, rulesService, policyService);
  }

  @Test
  void getAccountLimits_sinOpsCounter_devuelveCeroUsed() {
    com.nttdata.accountservice.model.entity.Account acc = new com.nttdata.accountservice.model.entity.Account();
    acc.setId("A1");
    acc.setFreeTransactionsLimit(2);
    acc.setCommissionFee(new BigDecimal("0.99"));
    acc.setOpsCounter(null);
    when(repository.findById("A1")).thenReturn(Mono.just(acc));

    StepVerifier.create(service.getAccountLimits("A1"))
        .assertNext(r -> {
          assertEquals(2, r.getFreeTransactionsLimit());
          assertEquals(0, new BigDecimal("0.99").compareTo(r.getCommissionFee()));
          assertEquals(0, r.getUsedTransactionsThisMonth());
        })
        .verifyComplete();
  }

  @Test
  void getAccountLimits_opsCounter_mesDistinto_reseteaUsados() {
    com.nttdata.accountservice.model.entity.Account acc = new com.nttdata.accountservice.model.entity.Account();
    acc.setId("A2");
    acc.setFreeTransactionsLimit(1);
    acc.setCommissionFee(new BigDecimal("1.50"));
    com.nttdata.accountservice.model.entity.OpsCounter oc = new com.nttdata.accountservice.model.entity.OpsCounter();
    oc.setYearMonth(YearMonth.now().minusMonths(1).toString());
    oc.setCount(7);
    acc.setOpsCounter(oc);
    when(repository.findById("A2")).thenReturn(Mono.just(acc));

    StepVerifier.create(service.getAccountLimits("A2"))
        .assertNext(r -> assertEquals(0, r.getUsedTransactionsThisMonth()))
        .verifyComplete();
  }

  @Test
  void applyBalanceOperation_opIds_trim_excede200() {
    com.nttdata.accountservice.model.entity.Account acc = new com.nttdata.accountservice.model.entity.Account();
    acc.setId("A3");
    acc.setAccountType("SAVINGS");
    acc.setBalance(new BigDecimal("100"));
    acc.setFreeTransactionsLimit(999);
    acc.setCommissionFee(new BigDecimal("0.10"));
    ArrayList<String> ids = new ArrayList<>();
    for (int i = 0; i < 200; i++) ids.add("op-" + i);
    acc.setOpIds(ids);

    when(repository.findById("A3")).thenReturn(Mono.just(acc));
    when(repository.save(any(com.nttdata.accountservice.model.entity.Account.class)))
        .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

    BalanceOperationRequest rq = new BalanceOperationRequest()
        .operationId("op-201")
        .type(BalanceOperationType.DEPOSIT)
        .amount(new BigDecimal("1"));

    StepVerifier.create(service.applyBalanceOperation("A3", rq))
        .assertNext(BalanceOperationResponse::getApplied)
        .verifyComplete();
  }
}
