package com.nttdata.account_service.service;

import com.nttdata.account_service.config.BusinessException;
import com.nttdata.account_service.model.AccountRequest;
import com.nttdata.account_service.model.entity.Account;
import com.nttdata.account_service.repository.AccountRepository;
import com.nttdata.account_service.service.rules.AccountRulesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

public class AccountRulesServiceTest {

    AccountRepository repo;
    AccountRulesService rules;

    @BeforeEach
    void setUp() {
        repo = mock(AccountRepository.class);
        rules = new AccountRulesService(repo);
    }

    private static Account a(String doc, String type) {
        Account x = new Account();
        x.setId(UUID.randomUUID().toString());
        x.setHolderDocument(doc);
        x.setAccountType(type);
        x.setActive(true);
        return x;
    }

    @Test
    void personal_conAhorrosPrevio_crearOtraAhorros_falla() {
        when(repo.findByHolderDocument("123")).thenReturn(Flux.fromIterable(List.of(a("123", "SAVINGS"))));

        StepVerifier.create(rules.validateLegacyRules("123", AccountRequest.AccountTypeEnum.SAVINGS, "PERSONAL"))
                .expectError(BusinessException.class)
                .verify();
    }

    @Test
    void personal_conCheckingPrevio_crearOtraChecking_falla() {
        when(repo.findByHolderDocument("123")).thenReturn(Flux.fromIterable(List.of(a("123", "CHECKING"))));

        StepVerifier.create(rules.validateLegacyRules("123", AccountRequest.AccountTypeEnum.CHECKING, "PERSONAL"))
                .expectError(BusinessException.class)
                .verify();
    }

    @Test
    void business_sinRestricciones_pasa() {
        when(repo.findByHolderDocument("RUC")).thenReturn(Flux.fromIterable(List.of(a("RUC", "CHECKING"), a("RUC", "CHECKING"))));

        StepVerifier.create(rules.validateLegacyRules("RUC", AccountRequest.AccountTypeEnum.CHECKING, "BUSINESS"))
                .verifyComplete();
    }
}
