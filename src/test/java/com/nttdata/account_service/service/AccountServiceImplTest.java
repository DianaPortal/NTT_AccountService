package com.nttdata.account_service.service;

import com.nttdata.account_service.config.BusinessException;
import com.nttdata.account_service.integration.credits.CreditsClient;
import com.nttdata.account_service.integration.customers.CustomersClient;
import com.nttdata.account_service.integration.customers.EligibilityResponse;
import com.nttdata.account_service.model.*;
import com.nttdata.account_service.model.entity.Account;
import com.nttdata.account_service.model.entity.OpsCounter;
import com.nttdata.account_service.repository.AccountRepository;
import com.nttdata.account_service.service.impl.AccountServiceImpl;
import com.nttdata.account_service.service.policy.AccountPolicyService;
import com.nttdata.account_service.service.rules.AccountRulesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


public class AccountServiceImplTest {

    AccountRepository accountRepository;
    CustomersClient customersClient;
    CreditsClient creditsClient;
    AccountPolicyService policyService;
    AccountRulesService accountRules;

    AccountServiceImpl service;

    @BeforeEach
    void init() {
        accountRepository = mock(AccountRepository.class);
        customersClient = mock(CustomersClient.class);
        creditsClient = mock(CreditsClient.class);
        policyService = mock(AccountPolicyService.class);
        accountRules = mock(AccountRulesService.class);

        service = new AccountServiceImpl(accountRepository, customersClient, creditsClient, accountRules, policyService);
    }

    private static EligibilityResponse elig(String type, String profile, String customerId) {
        EligibilityResponse e = new EligibilityResponse();
        e.setType(type);
        e.setProfile(profile);
        e.setCustomerId(customerId);
        return e;
    }

    private static AccountRequest req(String doc, AccountRequest.AccountTypeEnum type) {
        AccountRequest r = new AccountRequest();
        r.setHolderDocument(doc);
        r.setAccountType(type);
        r.setBalance(BigDecimal.ZERO);

        switch (type) {
            case SAVINGS:
                r.setMonthlyMovementLimit(5);
                r.setMaintenanceFee(null);
                r.setAllowedDayOfMonth(null);
                break;
            case CHECKING:
                r.setMaintenanceFee(new BigDecimal("5.00"));
                r.setMonthlyMovementLimit(null);
                r.setAllowedDayOfMonth(null);
                break;
            case FIXED_TERM:
                r.setAllowedDayOfMonth(10);
                r.setMonthlyMovementLimit(null);
                r.setMaintenanceFee(null);
                break;
        }
        return r;
    }

    //createAccount: beneficio VIP ahorros requiere tarjeta de crédito
    @Test
    void createAccount_savingsVipSinTarjeta_debeFallar422() {
        AccountRequest r = req("12345678", AccountRequest.AccountTypeEnum.SAVINGS);

        when(customersClient.getEligibilityByDocument("12345678"))
                .thenReturn(Mono.just(elig("PERSONAL", "VIP", "C1")));
        when(accountRules.validateLegacyRules("12345678", AccountRequest.AccountTypeEnum.SAVINGS, "PERSONAL"))
                .thenReturn(Mono.empty());
        when(creditsClient.hasActiveCreditCard("C1")).thenReturn(Mono.just(false));

        StepVerifier.create(service.createAccount(r))
                .expectError(BusinessException.class)
                .verify();

        verify(accountRepository, never()).save(any());
    }

    //  createAccount: ok -> se asignan números y defaults de política
    @Test
    void createAccount_ok_debePersistirConNumerosYDefaults() {
        AccountRequest r = req("12345678", AccountRequest.AccountTypeEnum.SAVINGS);

        when(customersClient.getEligibilityByDocument("12345678"))
                .thenReturn(Mono.just(elig("PERSONAL", "STANDARD", "C1")));
        when(accountRules.validateLegacyRules("12345678", AccountRequest.AccountTypeEnum.SAVINGS, "PERSONAL"))
                .thenReturn(Mono.empty());

        // Capturamos la entidad para validar números generados y defaults
        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(service.createAccount(r))
                .assertNext(resp -> {
                    assertNotNull(resp.getId());
                    assertNotNull(resp.getAccountNumber());
                    assertTrue(resp.getAccountNumber().matches("^\\d{11}$"), "accountNumber debe tener 11 dígitos");
                    assertNotNull(resp.getInterbankNumber());
                    assertTrue(resp.getInterbankNumber().matches("^\\d{20}$"), "interbankNumber debe tener 20 dígitos");
                })
                .verifyComplete();

        verify(policyService).applyDefaults(captor.capture(), eq(r.getAccountType()));
        Account saved = captor.getValue();
        assertNotNull(saved.getAccountType());
    }

    //  applyBalanceOperation
    @Test
    void applyBalance_aplicaComisionCuandoExcedeGratis() {
        Account acc = new Account();
        acc.setId("acc001");
        acc.setAccountType("CHECKING");
        acc.setBalance(new BigDecimal("100"));
        acc.setFreeTransactionsLimit(1);
        acc.setCommissionFee(new BigDecimal("2.00"));
        OpsCounter oc = new OpsCounter(); oc.setYearMonth(YearMonth.now().toString()); oc.setCount(1);
        acc.setOpsCounter(oc);
        acc.setOpIds(new ArrayList<>());

        when(accountRepository.findById("acc001")).thenReturn(Mono.just(acc));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        BalanceOperationRequest req = new BalanceOperationRequest()
                .amount(new BigDecimal("50"))
                .operationId("op-2")
                .type(BalanceOperationType.DEPOSIT);

        StepVerifier.create(service.applyBalanceOperation("acc001", req))
                .assertNext(r -> {
                    assertTrue(r.getApplied());
                    assertEquals(new BigDecimal("150"), r.getNewBalance());
                    assertEquals(new BigDecimal("2.00"), r.getCommissionApplied());
                })
                .verifyComplete();
    }

    // applyBalanceOperation: idempotencia
    @Test
    void applyBalance_idempotente_noAplica() {
        Account acc = new Account();
        acc.setId("acc001");
        acc.setAccountType("CHECKING");
        acc.setBalance(new BigDecimal("100"));
        acc.setFreeTransactionsLimit(10);
        acc.setCommissionFee(new BigDecimal("0.50"));
        OpsCounter oc = new OpsCounter(); oc.setYearMonth(YearMonth.now().toString()); oc.setCount(0);
        acc.setOpsCounter(oc);
        ArrayList<String> processed = new ArrayList<>();
        processed.add("op-1");
        acc.setOpIds(processed);

        when(accountRepository.findById("acc001")).thenReturn(Mono.just(acc));

        BalanceOperationRequest req = new BalanceOperationRequest()
                .amount(new BigDecimal("50"))
                .operationId("op-1")
                .type(BalanceOperationType.DEPOSIT);

        StepVerifier.create(service.applyBalanceOperation("acc001", req))
                .assertNext(r -> {
                    assertFalse(r.getApplied());
                    assertEquals(new BigDecimal("100"), r.getNewBalance());
                    assertEquals(BigDecimal.ZERO, r.getCommissionApplied());
                })
                .verifyComplete();

        verify(accountRepository, never()).save(any());
    }
}
