package com.nttdata.account_service.service;

import com.nttdata.account_service.model.AccountRequest;
import com.nttdata.account_service.model.AccountResponse;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface AccountService {
    Flux<AccountResponse> listAccounts();

    Mono<AccountResponse> getAccountById(String id);

    Mono<AccountResponse> createAccount(AccountRequest request);

    Mono<AccountResponse> updateAccount(String id, AccountRequest request);

    Mono<Void> deleteAccount(String id);
}
