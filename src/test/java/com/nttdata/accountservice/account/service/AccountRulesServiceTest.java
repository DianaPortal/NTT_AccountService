package com.nttdata.accountservice.account.service;


import com.nttdata.accountservice.model.*;
import com.nttdata.accountservice.model.entity.*;
import com.nttdata.accountservice.repository.*;
import com.nttdata.accountservice.service.rules.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.*;
import org.mockito.junit.jupiter.*;
import reactor.core.publisher.*;
import reactor.test.*;
import com.nttdata.accountservice.config.BusinessException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountRulesServiceTest {
  @Mock
  AccountRepository repository;

  @Test
  void personal_noPuedeTenerDosSavings() {
    Account existing = new Account();
    existing.setAccountType("SAVINGS");
    when(repository.findByHolderDocument("123")).thenReturn(Flux.just(existing));

    AccountRulesService service = new AccountRulesService(repository);

    StepVerifier.create(service.validateLegacyRules("123", AccountRequest.AccountTypeEnum.SAVINGS, "PERSONAL"))
        .expectErrorSatisfies(ex -> {
          assertInstanceOf(BusinessException.class, ex);
          assertTrue(ex.getMessage().contains("ya tiene una cuenta de tipo SAVINGS"));
        })
        .verify();
    verify(repository).findByHolderDocument("123");
  }

  @Test
  void business_noPuedeAbrirSavingsNiFixed() {
    when(repository.findByHolderDocument("RUC1")).thenReturn(Flux.empty());
    AccountRulesService service = new AccountRulesService(repository);

    StepVerifier.create(service.validateLegacyRules("RUC1", AccountRequest.AccountTypeEnum.SAVINGS, "BUSINESS"))
        .expectError(BusinessException.class)
        .verify();

    StepVerifier.create(service.validateLegacyRules("RUC1", AccountRequest.AccountTypeEnum.FIXED_TERM, "BUSINESS"))
        .expectErrorSatisfies(ex -> {
          assertInstanceOf(BusinessException.class, ex);
          assertTrue(ex.getMessage().contains("BUSINESS no puede abrir SAVINGS")); // o FIXED_TERM
        })
        .verify();
  }

  @Test
  void personal_puedeAbrirCheckingSiNoTiene() {
    when(repository.findByHolderDocument("123")).thenReturn(Flux.empty());
    AccountRulesService service = new AccountRulesService(repository);

    StepVerifier.create(service.validateLegacyRules("123", AccountRequest.AccountTypeEnum.CHECKING, "PERSONAL"))
        .verifyComplete();
  }

  @Test
  void personal_noPuedeTenerDosChecking() {
    Account existing = new Account();
    existing.setAccountType("CHECKING");
    when(repository.findByHolderDocument("456")).thenReturn(Flux.just(existing));

    AccountRulesService service = new AccountRulesService(repository);

    StepVerifier.create(service.validateLegacyRules("456", AccountRequest.AccountTypeEnum.CHECKING, "PERSONAL"))
        .expectErrorSatisfies(ex -> {
          assertInstanceOf(com.nttdata.accountservice.config.BusinessException.class, ex);
          assertTrue(ex.getMessage().contains("ya tiene una cuenta de tipo CHECKING"));
        })
        .verify();
  }

  @Test
  void business_puedeAbrirChecking() {
    when(repository.findByHolderDocument("RUC2")).thenReturn(Flux.empty());

    AccountRulesService service = new AccountRulesService(repository);

    StepVerifier.create(service.validateLegacyRules("RUC2", AccountRequest.AccountTypeEnum.CHECKING, "BUSINESS"))
        .verifyComplete();
  }

  @Test
  void personal_puedeAbrirSavingsSiNoTiene() {
    when(repository.findByHolderDocument("789")).thenReturn(Flux.empty());

    AccountRulesService service = new AccountRulesService(repository);

    StepVerifier.create(service.validateLegacyRules("789", AccountRequest.AccountTypeEnum.SAVINGS, "PERSONAL"))
        .verifyComplete();
  }


}
