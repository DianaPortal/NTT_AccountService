package com.nttdata.accountservice.account.service;

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
import org.springframework.web.server.*;
import reactor.core.publisher.*;
import reactor.test.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplUpdateAndDeleteTest {
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
  void updateAccount_ok() {
    com.nttdata.accountservice.model.entity.Account existing = new com.nttdata.accountservice.model.entity.Account();
    existing.setId("A1");
    existing.setMonthlyMovementLimit(5);

    when(repository.findById("A1")).thenReturn(Mono.just(existing));
    when(repository.save(any(com.nttdata.accountservice.model.entity.Account.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

    AccountRequest req = new AccountRequest()
        .holderDocument("12345678")
        .holderDocumentType(AccountRequest.HolderDocumentTypeEnum.DNI)
        .accountType(AccountRequest.AccountTypeEnum.SAVINGS)
        .monthlyMovementLimit(9);

    StepVerifier.create(service.updateAccount("A1", req))
        .assertNext(resp -> {
          assertEquals("A1", resp.getId());
          assertNotNull(resp.getMonthlyMovementLimit());
          assertTrue(resp.getMonthlyMovementLimit().isPresent());
          assertEquals(9, resp.getMonthlyMovementLimit().get().intValue());
        })
        .verifyComplete();

    ArgumentCaptor<com.nttdata.accountservice.model.entity.Account> captor = ArgumentCaptor.forClass(com.nttdata.accountservice.model.entity.Account.class);
    verify(repository).save(captor.capture());
    assertEquals(9, captor.getValue().getMonthlyMovementLimit());
  }

  @Test
  void updateAccount_404() {
    when(repository.findById("X")).thenReturn(Mono.empty());

    AccountRequest req = new AccountRequest()
        .holderDocument("12345678")
        .holderDocumentType(AccountRequest.HolderDocumentTypeEnum.DNI)
        .accountType(AccountRequest.AccountTypeEnum.SAVINGS)
        .monthlyMovementLimit(10);

    StepVerifier.create(service.updateAccount("X", req))
        .expectErrorSatisfies(ex -> assertInstanceOf(ResponseStatusException.class, ex))
        .verify();
  }

  @Test
  void deleteAccount_ok() {
    com.nttdata.accountservice.model.entity.Account e = new com.nttdata.accountservice.model.entity.Account();
    e.setId("A1");
    when(repository.findById("A1")).thenReturn(Mono.just(e));
    when(repository.deleteById("A1")).thenReturn(Mono.empty());

    StepVerifier.create(service.deleteAccount("A1")).verifyComplete();
    verify(repository).deleteById("A1");
  }

  @Test
  void deleteAccount_404() {
    when(repository.findById("Z")).thenReturn(Mono.empty());

    StepVerifier.create(service.deleteAccount("Z"))
        .expectErrorSatisfies(ex -> assertInstanceOf(ResponseStatusException.class, ex))
        .verify();
  }
}
