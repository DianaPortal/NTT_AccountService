package com.nttdata.account_service.service.impl;

import com.nttdata.account_service.api.ApiApiDelegate;
import com.nttdata.account_service.model.*;
import com.nttdata.account_service.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;

@Service
@RequiredArgsConstructor
public class AccountApiDelegateImpl implements ApiApiDelegate {

    private final AccountService accountService;

    @Override
    public Mono<ResponseEntity<AccountResponse>> registerAccount(
            Mono<AccountRequest> accountRequest, ServerWebExchange exchange) {
        return accountRequest
                .flatMap(accountService::createAccount)
                .map(resp -> ResponseEntity
                        .created(URI.create("/api/accounts/" + resp.getId()))
                        .body(resp));
    }

    @Override
    public Mono<ResponseEntity<InlineResponse200>> listAccounts(ServerWebExchange exchange) {
        return accountService.listAccounts()
                .collectList()
                .map(list -> new InlineResponse200().items(list))
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<AccountResponse>> getAccountById(String id, ServerWebExchange exchange) {
        return accountService.getAccountById(id).map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<AccountResponse>> updateAccount(
            String id, Mono<AccountRequest> accountRequest, ServerWebExchange exchange) {
        return accountRequest
                .flatMap(req -> accountService.updateAccount(id, req))
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Void>> deleteAccount(String id, ServerWebExchange exchange) {
        return accountService.deleteAccount(id).thenReturn(ResponseEntity.noContent().build());
    }

    @Override
    public Mono<ResponseEntity<AccountLimitsResponse>> getAccountLimits(String id, ServerWebExchange exchange) {
        return accountService.getAccountLimits(id)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<BalanceOperationResponse>> applyBalanceOperation(String id, Mono<BalanceOperationRequest> balanceOperationRequest, ServerWebExchange exchange) {
        return balanceOperationRequest
                .flatMap(req -> accountService.applyBalanceOperation(id, req))
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Flux<AccountResponse>>> listAccountsByHolderDocument(
            String document, ServerWebExchange exchange) {
        return accountService.getAccountsByHolderDocument(document)
                .collectList()
                .map(list -> list.isEmpty()
                        ? ResponseEntity.status(404).body(Flux.empty())
                        : ResponseEntity.ok(Flux.fromIterable(list)));
    }
}
