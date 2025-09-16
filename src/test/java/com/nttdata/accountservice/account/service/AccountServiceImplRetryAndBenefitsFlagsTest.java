package com.nttdata.accountservice.account.service;

import com.nttdata.accountservice.integration.credits.CreditsClient;
import com.nttdata.accountservice.integration.customers.CustomersClient;
import com.nttdata.accountservice.model.AccountRequest;
import com.nttdata.accountservice.integration.customers.EligibilityResponse;
import com.nttdata.accountservice.repository.AccountRepository;
import com.nttdata.accountservice.service.impl.AccountServiceImpl;
import com.nttdata.accountservice.service.policy.AccountPolicyService;
import com.nttdata.accountservice.service.rules.AccountRulesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplRetryAndBenefitsFlagsTest {
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
  void createAccount_retry_exhaustion_falla() {
    var req = new AccountRequest();
    req.setHolderDocument("12345678");
    req.setHolderDocumentType(AccountRequest.HolderDocumentTypeEnum.DNI);
    req.setAccountType(AccountRequest.AccountTypeEnum.SAVINGS);
    req.setMonthlyMovementLimit(5);

    EligibilityResponse elig = new EligibilityResponse();
    elig.setType("PERSONAL");
    elig.setProfile("STANDARD");
    when(customersClient.getEligibilityByDocument("DNI", "12345678")).thenReturn(Mono.just(elig));
    when(rulesService.validateLegacyRules(anyString(), any(), anyString())).thenReturn(Mono.empty());

    when(repository.save(any(com.nttdata.accountservice.model.entity.Account.class)))
        .thenAnswer(inv -> Mono.error(new DuplicateKeyException("dup1")))
        .thenAnswer(inv -> Mono.error(new DuplicateKeyException("dup2")))
        .thenAnswer(inv -> Mono.error(new DuplicateKeyException("dup3")))
        .thenAnswer(inv -> Mono.error(new DuplicateKeyException("dup4")));

    StepVerifier.create(service.createAccount(req))
        .expectError(DuplicateKeyException.class)
        .verify();
  }

  @Test
  void benefits_flags_desactivadas_no_exigen_TC() {
    // requireCcForVip = false; requireCcForPyme = false
    ReflectionTestUtils.setField(service, "requireCcForVip", false);
    ReflectionTestUtils.setField(service, "requireCcForPyme", false);

    var req = new AccountRequest();
    req.setHolderDocument("12345678");
    req.setHolderDocumentType(AccountRequest.HolderDocumentTypeEnum.DNI);
    req.setAccountType(AccountRequest.AccountTypeEnum.SAVINGS);
    req.setMonthlyMovementLimit(5);

    EligibilityResponse elig = new EligibilityResponse();
    elig.setType("PERSONAL");
    elig.setProfile("VIP");
    elig.setCustomerId("C1");

    when(customersClient.getEligibilityByDocument("DNI", "12345678")).thenReturn(Mono.just(elig));
    when(rulesService.validateLegacyRules(anyString(), any(), anyString())).thenReturn(Mono.empty());
    when(repository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

    StepVerifier.create(service.createAccount(req)).expectNextCount(1).verifyComplete();
    verifyNoInteractions(creditsClient);
  }
}
