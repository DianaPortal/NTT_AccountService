package com.nttdata.accountservice.account.service;

import com.nttdata.accountservice.config.*;
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

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AccountRulesServiceTest {
  @Mock
  AccountRepository repository;

  @Test
  void personal_noPuedeTenerDosSavings() {
    Account existing = new Account();
    existing.setAccountType("SAVINGS");
    when(repository.findByHolderDocument("123")).thenReturn(Flux.just(existing));

    AccountRulesService service = new AccountRulesService(repository);

    StepVerifier.create(service.validateLegacyRules("123", AccountRequest.AccountTypeEnum.SAVINGS, "PERSONAL"))
        .expectError(BusinessException.class)
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
        .expectError(BusinessException.class)
        .verify();
  }

  @Test
  void personal_puedeAbrirCheckingSiNoTiene() {
    when(repository.findByHolderDocument("123")).thenReturn(Flux.empty());
    AccountRulesService service = new AccountRulesService(repository);

    StepVerifier.create(service.validateLegacyRules("123", AccountRequest.AccountTypeEnum.CHECKING, "PERSONAL"))
        .verifyComplete();
  }
}
