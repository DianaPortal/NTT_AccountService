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
class AccountServiceImplCheckingValidationTest {

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
    // Desactivamos requisitos de tarjeta de crédito para cuentas VIP y PYMES para simplificar tests
    ReflectionTestUtils.setField(service, "requireCcForVip", false);
    ReflectionTestUtils.setField(service, "requireCcForPyme", false);
  }

  @Test
  void create_CHECKING_ok_validaCamposEspecificos() {
    var elig = new EligibilityResponse();
    elig.setCustomerId("C1");
    elig.setType("PERSONAL");
    elig.setProfile("STANDARD");

    when(customers.getEligibilityByDocument("DNI", "12345678")).thenReturn(Mono.just(elig));
    when(rules.validateLegacyRules(anyString(), any(), anyString())).thenReturn(Mono.empty());
    when(repository.save(any(com.nttdata.accountservice.model.entity.Account.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

    var req = new AccountRequest()
        .holderDocumentType(DNI)
        .holderDocument("12345678")
        .accountType(CHECKING)
        .monthlyMovementLimit(10)
        .maintenanceFee(new BigDecimal("5.00"));

    StepVerifier.create(service.createAccount(req))
        .assertNext(resp -> assertEquals("12345678", resp.getHolderDocument()))
        .verifyComplete();
  }

  @Test
  void create_CHECKING_falla_si_allowedDayOfMonth_presente() {
    var elig = new EligibilityResponse();
    elig.setCustomerId("c1");
    elig.setType("PERSONAL");
    elig.setProfile("STANDARD");

    var req = new AccountRequest()
        .holderDocumentType(DNI)
        .holderDocument("12345678")
        .accountType(CHECKING)
        .monthlyMovementLimit(10)
        .maintenanceFee(new BigDecimal("5.00"))
        .allowedDayOfMonth(15);

    StepVerifier.create(service.createAccount(req))
        .expectErrorSatisfies(ex -> {
          assertInstanceOf(BusinessException.class, ex);
          assertEquals("allowedDayOfMonth no debe estar presente en cuentas corrientes", ex.getMessage());
        })
        .verify();
  }
}

