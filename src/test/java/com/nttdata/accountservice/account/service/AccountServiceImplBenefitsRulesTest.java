package com.nttdata.accountservice.account.service;

/*
 * Reglas de beneficios createAccount
 * VIP / PYME con requerimientos de tarjeta de crédito dependiendo de flags.
 */

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
  void savingsVIP_requiereTC_activa_false_lanzaError() {
    ReflectionTestUtils.setField(service, "requireCcForVip", true);
    var elig = new EligibilityResponse();
    elig.setCustomerId("C1B");
    elig.setType("PERSONAL");
    elig.setProfile("VIP");
    when(rules.validateLegacyRules(anyString(), any(), anyString())).thenReturn(Mono.empty());
    when(customers.getEligibilityByDocument("DNI", "99998888")).thenReturn(Mono.just(elig));
    when(credits.hasActiveCreditCard("C1B")).thenReturn(Mono.just(false));
    var req = new AccountRequest()
        .holderDocumentType(DNI)
        .holderDocument("99998888")
        .accountType(SAVINGS)
        .monthlyMovementLimit(3);

    StepVerifier.create(service.createAccount(req))
        .expectErrorSatisfies(ex -> {
          assertInstanceOf(BusinessException.class, ex);
          assertTrue(ex.getMessage().contains("Ahorro VIP requiere"));
        })
        .verify();
  }

  @Test
  void savingsVIP_flagDeshabilitado_noSolicitaTC() {
    ReflectionTestUtils.setField(service, "requireCcForVip", false); // deshabilitado
    var elig = new EligibilityResponse();
    elig.setCustomerId("C1C");
    elig.setType("PERSONAL");
    elig.setProfile("VIP");
    when(rules.validateLegacyRules(anyString(), any(), anyString())).thenReturn(Mono.empty());
    when(customers.getEligibilityByDocument("DNI", "77776666")).thenReturn(Mono.just(elig));
    // NO se debe llamar a credits.hasActiveCreditCard
    when(repository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
    var req = new AccountRequest()
        .holderDocumentType(DNI)
        .holderDocument("77776666")
        .accountType(SAVINGS)
        .monthlyMovementLimit(2);

    StepVerifier.create(service.createAccount(req))
        .expectNextCount(1)
        .verifyComplete();
    verify(credits, never()).hasActiveCreditCard(anyString());
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

  @Test
  void checkingPYME_flagDeshabilitado_noSolicitaTC() {
    ReflectionTestUtils.setField(service, "requireCcForPyme", false); // deshabilitado
    var elig = new EligibilityResponse();
    elig.setCustomerId("CPN");
    elig.setType("BUSINESS");
    elig.setProfile("PYME");
    when(rules.validateLegacyRules(anyString(), any(), anyString())).thenReturn(Mono.empty());
    when(customers.getEligibilityByDocument("DNI", "55554444")).thenReturn(Mono.just(elig));
    when(repository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

    var req = new AccountRequest()
        .holderDocumentType(DNI)
        .holderDocument("55554444")
        .accountType(CHECKING)
        .maintenanceFee(BigDecimal.ONE)
        .monthlyMovementLimit(9);

    StepVerifier.create(service.createAccount(req))
        .expectNextCount(1)
        .verifyComplete();
    verify(credits, never()).hasActiveCreditCard(anyString());
  }

  @Test
  void checkingPYME_conMaintenanceFee_usuarioLoEnvia_seFuerzaACero() {
    // Flag obliga a verificar TC, pero simulamos que sí la tiene activa
    ReflectionTestUtils.setField(service, "requireCcForPyme", true);
    var elig = new EligibilityResponse();
    elig.setCustomerId("C3");
    elig.setType("BUSINESS");
    elig.setProfile("PYME");
    when(rules.validateLegacyRules(anyString(), any(), anyString())).thenReturn(Mono.empty());
    when(customers.getEligibilityByDocument("DNI", "11112222")).thenReturn(Mono.just(elig));
    when(credits.hasActiveCreditCard("C3")).thenReturn(Mono.just(true));
    when(repository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

    var req = new AccountRequest()
        .holderDocumentType(DNI)
        .holderDocument("11112222")
        .accountType(CHECKING)
        .maintenanceFee(new BigDecimal("50")) // será sobreescrita a 0
        .monthlyMovementLimit(5);

    StepVerifier.create(service.createAccount(req))
        .assertNext(resp -> {
          assertEquals(0, BigDecimal.ZERO.compareTo(resp.getMaintenanceFee().orElse(BigDecimal.ZERO)));
          assertEquals(CHECKING.name(), resp.getAccountType().name());
        })
        .verifyComplete();
  }

  @Test
  void fixedTerm_happyPath_allowedDayWithinRange_creaCorrecto() {
    var elig = new EligibilityResponse();
    elig.setCustomerId("C4");
    elig.setType("PERSONAL");
    elig.setProfile("STANDARD"); // no VIP ni PYME
    when(rules.validateLegacyRules(anyString(), any(), anyString())).thenReturn(Mono.empty());
    when(customers.getEligibilityByDocument("DNI", "22334455")).thenReturn(Mono.just(elig));
    when(repository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

    var req = new AccountRequest()
        .holderDocumentType(DNI)
        .holderDocument("22334455")
        .accountType(FIXED_TERM)
        .allowedDayOfMonth(15);

    StepVerifier.create(service.createAccount(req))
        .assertNext(resp -> {
          assertEquals(FIXED_TERM.name(), resp.getAccountType().name());
          assertEquals(15, resp.getAllowedDayOfMonth().orElse(null));
        })
        .verifyComplete();
  }

  @Test
  void legacyRules_error_propagadoAntesBeneficios() {
    // Fuerza error en legacy para cubrir rama legacy.then(benefit) fallando antes de benefit
    when(rules.validateLegacyRules(anyString(), any(),isNull()))
        .thenReturn(Mono.error(new BusinessException("legacy fail")));
    when(customers.getEligibilityByDocument(anyString(), anyString()))
        .thenReturn(Mono.just(new EligibilityResponse()));
    var req = new AccountRequest()
        .holderDocumentType(DNI)
        .holderDocument("11223344")
        .accountType(SAVINGS)
        .monthlyMovementLimit(5);

    StepVerifier.create(service.createAccount(req))
        .expectErrorSatisfies(ex -> {
          assertInstanceOf(BusinessException.class, ex);
          assertTrue(ex.getMessage().contains("legacy fail"));
        })
        .verify();
    verifyNoInteractions(credits, repository);
  }
}

