package com.nttdata.account_service.service;

import com.nttdata.account_service.api.ApiApiDelegate;
import com.nttdata.account_service.model.AccountRequest;
import com.nttdata.account_service.model.AccountResponse;
import com.nttdata.account_service.model.InlineResponse200;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AccountApiDelegateImpl implements ApiApiDelegate {

    private final AccountService accountService;

    @Override
    public Mono<ResponseEntity<AccountResponse>> registerAccount(
            Mono<AccountRequest> accountRequest, ServerWebExchange exchange) {
        return accountRequest
                .flatMap(accountService::createAccount)
                .map(ResponseEntity::ok)
                .onErrorResume(IllegalArgumentException.class,
                        e -> Mono.just(ResponseEntity.badRequest().build()));
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
        return accountService.getAccountById(id)
                .map(ResponseEntity::ok)
                .onErrorResume(IllegalArgumentException.class,
                        e -> Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).build()));
    }

    @Override
    public Mono<ResponseEntity<AccountResponse>> updateAccount(
            String id, Mono<AccountRequest> accountRequest, ServerWebExchange exchange) {
        return accountRequest
                .flatMap(req -> accountService.updateAccount(id, req))
                .map(ResponseEntity::ok)
                .onErrorResume(IllegalArgumentException.class,
                        e -> Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).build()));
    }


}
