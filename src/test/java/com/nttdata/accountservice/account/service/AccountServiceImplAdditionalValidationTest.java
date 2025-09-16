package com.nttdata.accountservice.account.service;

import com.nttdata.accountservice.config.BusinessException;
import com.nttdata.accountservice.integration.credits.CreditsClient;
import com.nttdata.accountservice.integration.customers.CustomersClient;
import com.nttdata.accountservice.model.AccountRequest;
import com.nttdata.accountservice.repository.AccountRepository;
import com.nttdata.accountservice.service.impl.AccountServiceImpl;
import com.nttdata.accountservice.service.policy.AccountPolicyService;
import com.nttdata.accountservice.service.rules.AccountRulesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static com.nttdata.accountservice.model.AccountRequest.AccountTypeEnum.*;
import static com.nttdata.accountservice.model.AccountRequest.HolderDocumentTypeEnum.DNI;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplAdditionalValidationTest {
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
  void holderDocument_regex_invalido_lanza_error() {
    var req = new AccountRequest()
        .holderDocumentType(DNI)
        .holderDocument("123") // muy corto
        .accountType(SAVINGS);

    StepVerifier.create(service.createAccount(req))
        .expectErrorSatisfies(ex -> {
          assertInstanceOf(BusinessException.class, ex);
          assertEquals("El documento debe tener entre 8 y 11 dígitos", ex.getMessage());
        })
        .verify();
  }

  @Test
  void savings_con_maintenanceFee_debe_fallar() {
    var req = new AccountRequest()
        .holderDocumentType(DNI)
        .holderDocument("12345678")
        .accountType(SAVINGS)
        .monthlyMovementLimit(10)
        .maintenanceFee(BigDecimal.TEN);

    StepVerifier.create(service.createAccount(req))
        .expectErrorSatisfies(ex -> {
          assertInstanceOf(BusinessException.class, ex);
          assertEquals("maintenanceFee no debe estar presente en cuentas de ahorro", ex.getMessage());
        })
        .verify();
  }

  @Test
  void savings_con_allowedDayOfMonth_debe_fallar() {
    var req = new AccountRequest()
        .holderDocumentType(DNI)
        .holderDocument("12345678")
        .accountType(SAVINGS)
        .monthlyMovementLimit(10)
        .allowedDayOfMonth(10);

    StepVerifier.create(service.createAccount(req))
        .expectErrorSatisfies(ex -> {
          assertInstanceOf(BusinessException.class, ex);
          assertEquals("allowedDayOfMonth no debe estar presente en cuentas de ahorro", ex.getMessage());
        })
        .verify();
  }

  @Test
  void checking_sin_monthlyMovementLimit_debe_fallar() {
    var req = new AccountRequest()
        .holderDocumentType(DNI)
        .holderDocument("12345678")
        .accountType(CHECKING)
        .maintenanceFee(BigDecimal.ONE);

    StepVerifier.create(service.createAccount(req))
        .expectErrorSatisfies(ex -> {
          assertInstanceOf(BusinessException.class, ex);
          assertEquals("monthlyMovementLimit es obligatorio para cuentas corrientes", ex.getMessage());
        })
        .verify();
  }

  @Test
  void checking_sin_maintenanceFee_debe_fallar() {
    var req = new AccountRequest()
        .holderDocumentType(DNI)
        .holderDocument("12345678")
        .accountType(CHECKING)
        .monthlyMovementLimit(10);

    StepVerifier.create(service.createAccount(req))
        .expectErrorSatisfies(ex -> {
          assertInstanceOf(BusinessException.class, ex);
          assertEquals("maintenanceFee es obligatorio para cuentas corrientes", ex.getMessage());
        })
        .verify();
  }

  @Test
  void fixedTerm_sin_allowedDayOfMonth_debe_fallar() {
    var req = new AccountRequest()
        .holderDocumentType(DNI)
        .holderDocument("12345678")
        .accountType(FIXED_TERM);

    StepVerifier.create(service.createAccount(req))
        .expectErrorSatisfies(ex -> {
          assertInstanceOf(BusinessException.class, ex);
          assertEquals("allowedDayOfMonth es obligatorio para cuentas a plazo fijo", ex.getMessage());
        })
        .verify();
  }

  @Test
  void fixedTerm_allowedDayOfMonth_fuera_de_rango_inferior() {
    var req = new AccountRequest()
        .holderDocumentType(DNI)
        .holderDocument("12345678")
        .accountType(FIXED_TERM)
        .allowedDayOfMonth(0);

    StepVerifier.create(service.createAccount(req))
        .expectErrorSatisfies(ex -> {
          assertInstanceOf(BusinessException.class, ex);
          assertEquals("allowedDayOfMonth debe estar entre 1 y 28", ex.getMessage());
        })
        .verify();
  }

  @Test
  void fixedTerm_allowedDayOfMonth_fuera_de_rango_superior() {
    var req = new AccountRequest()
        .holderDocumentType(DNI)
        .holderDocument("12345678")
        .accountType(FIXED_TERM)
        .allowedDayOfMonth(29);

    StepVerifier.create(service.createAccount(req))
        .expectErrorSatisfies(ex -> {
          assertInstanceOf(BusinessException.class, ex);
          assertEquals("allowedDayOfMonth debe estar entre 1 y 28", ex.getMessage());
        })
        .verify();
  }

  @Test
  void fixedTerm_con_monthlyMovementLimit_debe_fallar() {
    var req = new AccountRequest()
        .holderDocumentType(DNI)
        .holderDocument("12345678")
        .accountType(FIXED_TERM)
        .allowedDayOfMonth(10)
        .monthlyMovementLimit(5);

    StepVerifier.create(service.createAccount(req))
        .expectErrorSatisfies(ex -> {
          assertInstanceOf(BusinessException.class, ex);
          assertEquals("monthlyMovementLimit no debe estar presente en cuentas a plazo fijo", ex.getMessage());
        })
        .verify();
  }
}
