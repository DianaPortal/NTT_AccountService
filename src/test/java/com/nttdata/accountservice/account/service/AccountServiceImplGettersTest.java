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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AccountServiceImplGettersTest {
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
  void getAccountLimits_404() {
    when(repository.findById("NOPE")).thenReturn(Mono.empty());

    StepVerifier.create(service.getAccountLimits("NOPE"))
        .expectErrorSatisfies(ex -> assertInstanceOf(ResponseStatusException.class, ex))
        .verify();
  }

  @Test
  void getAccountsByHolderDocument_ok() {
    Account e = new Account();
    e.setId("A1");
    e.setHolderDocument("123");
    when(repository.findByHolderDocument("123")).thenReturn(Flux.just(e));

    StepVerifier.create(service.getAccountsByHolderDocument("123"))
        .assertNext(r -> assertEquals("A1", r.getId()))
        .verifyComplete();
  }
}
