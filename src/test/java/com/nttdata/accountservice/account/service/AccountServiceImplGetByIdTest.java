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
class AccountServiceImplGetByIdTest {

  @Mock
  AccountRepository repository;
  @Mock
  CustomersClient customers;
  @Mock
  CreditsClient credits;
  @Mock
  AccountRulesService rules;
  @Mock
  AccountPolicyService policy;

  AccountServiceImpl service;

  @BeforeEach
  void init() {
    service = new AccountServiceImpl(repository, customers, credits, rules, policy);
  }

  @Test
  void getAccountById_ok() {
    Account e = new Account();
    e.setId("ID123");
    when(repository.findById("ID123")).thenReturn(Mono.just(e));

    StepVerifier.create(service.getAccountById("ID123"))
        .assertNext(r -> assertEquals("ID123", r.getId()))
        .verifyComplete();
  }

  @Test
  void getAccountById_404() {
    when(repository.findById("Z")).thenReturn(Mono.empty());

    StepVerifier.create(service.getAccountById("Z"))
        .expectError(ResponseStatusException.class)
        .verify();
  }
}
