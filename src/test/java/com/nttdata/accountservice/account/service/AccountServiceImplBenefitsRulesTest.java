package com.nttdata.accountservice.account.service;

import com.nttdata.accountservice.config.*;
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
import org.springframework.test.util.*;
import reactor.core.publisher.*;
import reactor.test.*;

import java.math.*;

import static com.nttdata.accountservice.model.AccountRequest.AccountTypeEnum.*;
import static com.nttdata.accountservice.model.AccountRequest.HolderDocumentTypeEnum.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplBenefitsRulesTest {

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
  void savingsVIP_requiereTC_activa_true_permiteCrear() {
    ReflectionTestUtils.setField(service, "requireCcForVip", true);
    var elig = new EligibilityResponse();
    elig.setCustomerId("C1");
    elig.setType("PERSONAL");
    elig.setProfile("VIP");
    when(rules.validateLegacyRules(anyString(), any(), anyString())).thenReturn(Mono.empty());
    when(customers.getEligibilityByDocument("DNI", "12345678")).thenReturn(Mono.just(elig));
    when(credits.hasActiveCreditCard("C1")).thenReturn(Mono.just(true));
    when(repository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
    var req = new AccountRequest()
        .holderDocumentType(DNI)
        .holderDocument("12345678")
        .accountType(SAVINGS)
        .monthlyMovementLimit(10);

    StepVerifier.create(service.createAccount(req)).expectNextCount(1).verifyComplete();
  }

  @Test
  void checkingPYME_requiereTC_activa_false_lanza422() {
    ReflectionTestUtils.setField(service, "requireCcForPyme", true);
    var elig = new EligibilityResponse();
    elig.setCustomerId("C2");
    elig.setType("BUSINESS");
    elig.setProfile("PYME");

    when(rules.validateLegacyRules(anyString(), any(), anyString())).thenReturn(Mono.empty());
    when(customers.getEligibilityByDocument("DNI", "87654321")).thenReturn(Mono.just(elig));
    when(credits.hasActiveCreditCard("C2")).thenReturn(Mono.just(false));

    var req = new AccountRequest()
        .holderDocumentType(DNI)
        .holderDocument("87654321")
        .accountType(CHECKING)
        .maintenanceFee(BigDecimal.valueOf(10))
        .monthlyMovementLimit(10);


    StepVerifier.create(service.createAccount(req))
        .expectErrorSatisfies(ex -> {
          assertInstanceOf(BusinessException.class, ex);
          assertTrue(ex.getMessage().contains("PYME requiere Tarjeta de Crédito activa"));
        })
        .verify();
  }
}

