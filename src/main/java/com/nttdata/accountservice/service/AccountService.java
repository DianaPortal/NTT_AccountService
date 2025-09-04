package com.nttdata.accountservice.service;

import com.nttdata.accountservice.model.*;
import reactor.core.publisher.*;

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
