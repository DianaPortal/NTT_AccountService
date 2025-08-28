package com.nttdata.account_service.service;

import com.nttdata.account_service.model.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AccountService {
    Flux<AccountResponse> listAccounts();

    Mono<AccountResponse> getAccountById(String id);

    Mono<AccountResponse> createAccount(AccountRequest request);

    Mono<AccountResponse> updateAccount(String id, AccountRequest request);

    Mono<Void> deleteAccount(String id);

    Mono<AccountLimitsResponse> getAccountLimits(String id);

    Mono<BalanceOperationResponse> applyBalanceOperation(String accountId, BalanceOperationRequest request);

    Flux<AccountResponse> getAccountsByHolderDocument(String holderDocument);

}
