package com.nttdata.accountservice.account.service;

/*
 * Validaciones base de createAccount
 * Campos obligatorios, formato documento, balance inicial negativo.
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
import reactor.test.*;

import java.math.*;

import static com.nttdata.accountservice.model.AccountRequest.AccountTypeEnum.*;
import static com.nttdata.accountservice.model.AccountRequest.HolderDocumentTypeEnum.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 */
@ExtendWith(MockitoExtension.class)
class AccountServiceImplValidationTest {
  @Mock
  AccountRepository repository;
  @Mock
  CustomersClient customersClient;
  @Mock
  CreditsClient creditsClient;
  @Mock
  AccountRulesService accountRules;
  @Mock
  AccountPolicyService policyService;

  AccountServiceImpl service;

  @BeforeEach
  void init() {
    service = new AccountServiceImpl(repository, customersClient, creditsClient, accountRules, policyService);
  }


  @Test
  void request_nulo_lanza_excepcion() {
    StepVerifier.create(service.createAccount(null))
        .expectErrorSatisfies(ex -> {
          assertInstanceOf(BusinessException.class, ex);
          assertEquals("AccountRequest no puede ser nulo", ex.getMessage());
        })
        .verify();
  }

  @Test
  void holderDocumentType_obligatorio() {
    var req = new AccountRequest()
        .holderDocument("12345678")
        .accountType(SAVINGS);

    StepVerifier.create(service.createAccount(req))
        .expectErrorSatisfies(ex -> {
          assertInstanceOf(BusinessException.class, ex);
          assertEquals("holderDocumentType es obligatorio", ex.getMessage());
        })
        .verify();
  }

  @Test
  void holderDocument_obligatorio() {
    var req = new AccountRequest()
        .holderDocumentType(DNI)
        .accountType(SAVINGS);

    StepVerifier.create(service.createAccount(req))
        .expectErrorSatisfies(ex -> {
          assertInstanceOf(BusinessException.class, ex);
          assertEquals("holderDocument es obligatorio", ex.getMessage());
        })
        .verify();
  }

  @Test
  void accountType_obligatorio() {
    var req = new AccountRequest()
        .holderDocumentType(DNI)
        .holderDocument("12345678");

    StepVerifier.create(service.createAccount(req))
        .expectErrorSatisfies(ex -> {
          assertInstanceOf(BusinessException.class, ex);
          assertEquals("accountType es obligatorio", ex.getMessage());
        })
        .verify();
  }

  @Test
  void holderDocument_vacio() {
    var req = new AccountRequest()
        .holderDocumentType(DNI)
        .holderDocument("")
        .accountType(SAVINGS);

    StepVerifier.create(service.createAccount(req))
        .expectErrorSatisfies(ex -> {
          assertInstanceOf(BusinessException.class, ex);
          assertEquals("holderDocument es obligatorio", ex.getMessage());
        })
        .verify();
  }

  @Test
  void holderDocument_null() {
    var req = new AccountRequest()
        .holderDocumentType(DNI)
        .holderDocument(null)
        .accountType(SAVINGS);

    StepVerifier.create(service.createAccount(req))
        .expectErrorSatisfies(ex -> {
          assertInstanceOf(BusinessException.class, ex);
          assertEquals("holderDocument es obligatorio", ex.getMessage());
        })
        .verify();
  }

  @Test
  void balance_negativo_debe_fallar() {
    var req = new AccountRequest()
        .holderDocumentType(DNI)
        .holderDocument("12345678")
        .accountType(SAVINGS)
        .balance(BigDecimal.valueOf(-100));

    StepVerifier.create(service.createAccount(req))
        .expectErrorSatisfies(ex -> {
          assertInstanceOf(BusinessException.class, ex);
          assertEquals("El balance inicial no puede ser negativo", ex.getMessage());
        })
        .verify();
  }


  @Test
  void savings_sin_monthlyMovementLimit_debe_fallar() {
    var req = new AccountRequest()
        .holderDocumentType(DNI)
        .holderDocument("12345678")
        .accountType(SAVINGS)
        .allowedDayOfMonth(15); // pero sin monthlyMovementLimit

    StepVerifier.create(service.createAccount(req))
        .expectErrorSatisfies(ex -> {
          assertInstanceOf(BusinessException.class, ex);
          assertEquals("monthlyMovementLimit es obligatorio para cuentas de ahorro", ex.getMessage());
        })
        .verify();
  }


  @Test
  void fixedTerm_con_maintenanceFee_debe_fallar() {
    var req = new AccountRequest()
        .holderDocumentType(DNI)
        .holderDocument("12345678")
        .accountType(FIXED_TERM)
        .allowedDayOfMonth(15)
        .maintenanceFee(BigDecimal.valueOf(10));

    StepVerifier.create(service.createAccount(req))
        .expectErrorSatisfies(ex -> {
          assertInstanceOf(BusinessException.class, ex);
          assertEquals("maintenanceFee no debe estar presente en cuentas a plazo fijo", ex.getMessage());
        })
        .verify();
  }

}
