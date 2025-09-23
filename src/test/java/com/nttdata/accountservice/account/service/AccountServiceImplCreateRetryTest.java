package com.nttdata.accountservice.account.service;

/*
 * createAccount retry / trySaveWithRetry
 * Cubre reintento tras DuplicateKeyException y éxito final.
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
import org.springframework.dao.*;
import reactor.core.publisher.*;
import reactor.test.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplCreateRetryTest {
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
  void createAccount_retryOnDuplicateKey_generatesNewNumbers() {
    AccountRequest req = new AccountRequest();
    req.setHolderDocument("12345678");
    req.setHolderDocumentType(AccountRequest.HolderDocumentTypeEnum.DNI);
    req.setAccountType(AccountRequest.AccountTypeEnum.SAVINGS);
    req.setMonthlyMovementLimit(5);


    when(customersClient.getEligibilityByDocument("DNI", "12345678"))
        .thenReturn(Mono.just(new EligibilityResponse()));
    when(rulesService.validateLegacyRules(any(), any(), any()))
        .thenReturn(Mono.empty());

    when(repository.save(any(com.nttdata.accountservice.model.entity.Account.class)))
        .thenAnswer(inv -> Mono.error(new DuplicateKeyException("dup")))
        .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

    StepVerifier.create(service.createAccount(req))
        .assertNext(resp -> {
          assertNotNull(resp.getAccountNumber());
          assertTrue(resp.getAccountNumber().matches("\\d{10,20}"));
          assertNotNull(resp.getInterbankNumber());
          assertTrue(resp.getInterbankNumber().matches("\\d{10,30}"));
        })
        .verifyComplete();

    verify(repository, times(2)).save(any(com.nttdata.accountservice.model.entity.Account.class));
  }

  @Test
  void createAccount_retryExhausted_lanzaErrorFinal() {
    AccountRequest req = new AccountRequest();
    req.setHolderDocument("87654321");
    req.setHolderDocumentType(AccountRequest.HolderDocumentTypeEnum.DNI);
    req.setAccountType(AccountRequest.AccountTypeEnum.SAVINGS);
    req.setMonthlyMovementLimit(5);

    when(customersClient.getEligibilityByDocument("DNI", "87654321"))
        .thenReturn(Mono.just(new EligibilityResponse()));
    when(rulesService.validateLegacyRules(any(), any(), any()))
        .thenReturn(Mono.empty());

    when(repository.save(any(com.nttdata.accountservice.model.entity.Account.class)))
        .thenAnswer(inv -> Mono.error(new DuplicateKeyException("dup1")))
        .thenAnswer(inv -> Mono.error(new DuplicateKeyException("dup2")))
        .thenAnswer(inv -> Mono.error(new DuplicateKeyException("dup3")))
        .thenAnswer(inv -> Mono.error(new DuplicateKeyException("dup4-final"))); // después de 3 reintentos debe propagarse

    StepVerifier.create(service.createAccount(req))
        .expectErrorSatisfies(ex -> assertInstanceOf(DuplicateKeyException.class, ex))
        .verify();

    verify(repository, atLeast(4)).save(any(com.nttdata.accountservice.model.entity.Account.class));
  }
}
