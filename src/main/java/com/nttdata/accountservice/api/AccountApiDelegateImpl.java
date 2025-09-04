package com.nttdata.accountservice.api;


import com.nttdata.accountservice.model.*;
import com.nttdata.accountservice.service.*;
import lombok.*;
import org.springframework.http.*;
import org.springframework.stereotype.*;
import org.springframework.web.server.*;
import reactor.core.publisher.*;

import java.net.*;

/**
 * Implementación del delegate de la API de cuentas.
 */

@Service
@RequiredArgsConstructor
public class AccountApiDelegateImpl implements ApiApiDelegate {

  private final AccountService service;

  @Override
  public Mono<ResponseEntity<AccountResponse>> registerAccount(
      Mono<AccountRequest> accountRequest, ServerWebExchange exchange) {
    return accountRequest
        .flatMap(service::createAccount)
        .map(resp -> ResponseEntity
            .created(URI.create("/api/accounts/" + resp.getId()))
            .body(resp));
  }

  @Override
  public Mono<ResponseEntity<InlineResponse200>> listAccounts(
      ServerWebExchange exchange) {
    return service.listAccounts()
        .collectList()
        .map(list -> new InlineResponse200().items(list))
        .map(ResponseEntity::ok);
  }

  @Override
  public Mono<ResponseEntity<AccountResponse>> getAccountById(
      String id, ServerWebExchange exchange) {
    return service.getAccountById(id).map(ResponseEntity::ok);
  }

  @Override
  public Mono<ResponseEntity<AccountResponse>> updateAccount(
      String id, Mono<AccountRequest> accountRequest, ServerWebExchange exchange) {
    return accountRequest
        .flatMap(req -> service.updateAccount(id, req))
        .map(ResponseEntity::ok);
  }

  @Override
  public Mono<ResponseEntity<Void>> deleteAccount(
      String id, ServerWebExchange exchange) {
    return service.deleteAccount(id).thenReturn(ResponseEntity.noContent().build());
  }

  @Override
  public Mono<ResponseEntity<AccountLimitsResponse>> getAccountLimits(
      String id, ServerWebExchange exchange) {
    return service.getAccountLimits(id)
        .map(ResponseEntity::ok);
  }

  @Override
  public Mono<ResponseEntity<BalanceOperationResponse>> applyBalanceOperation(
      String id, Mono<BalanceOperationRequest> balanceOperationRequest, ServerWebExchange exchange) {
    return balanceOperationRequest
        .flatMap(req -> service.applyBalanceOperation(id, req))
        .map(ResponseEntity::ok);
  }

  @Override
  public Mono<ResponseEntity<Flux<AccountResponse>>> listAccountsByHolderDocument(
      String document, ServerWebExchange exchange) {
    return service.getAccountsByHolderDocument(document)
        .collectList()
        .map(list -> list.isEmpty()
            ? ResponseEntity.status(404).body(Flux.empty())
            : ResponseEntity.ok(Flux.fromIterable(list)));
  }
}
