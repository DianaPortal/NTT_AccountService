package com.nttdata.accountservice.account.service;

/*
 *
 * - getAccountLimits edge cases (contador nulo, mes distinto)
 * - applyBalanceOperation: trim de opIds (>200) y verificación de contador
 */

import com.nttdata.accountservice.integration.credits.*;
import com.nttdata.accountservice.integration.customers.*;
import com.nttdata.accountservice.model.*;
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
import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplLimitsAndOpsEdgeTest {
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
