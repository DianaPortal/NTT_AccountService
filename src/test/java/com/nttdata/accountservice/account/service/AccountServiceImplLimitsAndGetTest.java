package com.nttdata.accountservice.account.service;

import com.nttdata.accountservice.integration.credits.*;
import com.nttdata.accountservice.integration.customers.*;
import com.nttdata.accountservice.model.entity.*;
import com.nttdata.accountservice.repository.*;
import com.nttdata.accountservice.service.impl.*;
import com.nttdata.accountservice.service.policy.*;
import com.nttdata.accountservice.service.rules.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.*;
import org.mockito.junit.jupiter.*;
import org.springframework.web.server.*;
import reactor.core.publisher.*;
import reactor.test.*;

import java.math.*;
import java.time.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AccountServiceImplLimitsAndGetTest {
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
  void getAccountLimits_ok() {
    Account acc = new Account();
    acc.setId("A1");
    acc.setFreeTransactionsLimit(3);
    acc.setCommissionFee(new BigDecimal("1.25"));
    OpsCounter oc = new OpsCounter();
    oc.setYearMonth(YearMonth.now().toString());
    oc.setCount(1);
    acc.setOpsCounter(oc);

    when(repository.findById("A1")).thenReturn(Mono.just(acc));

    StepVerifier.create(service.getAccountLimits("A1"))
        .assertNext(r -> {
          assertEquals(3, r.getFreeTransactionsLimit());
          assertEquals(0, new BigDecimal("1.25").compareTo(r.getCommissionFee()));
          assertEquals(1, r.getUsedTransactionsThisMonth());
        })
        .verifyComplete();

    verify(repository).findById("A1");
  }

  @Test
  void getAccountLimits_404() {
    when(repository.findById("X")).thenReturn(Mono.empty());

    StepVerifier.create(service.getAccountLimits("X"))
        .expectErrorSatisfies(ex -> assertInstanceOf(ResponseStatusException.class, ex))
        .verify();
  }
}
