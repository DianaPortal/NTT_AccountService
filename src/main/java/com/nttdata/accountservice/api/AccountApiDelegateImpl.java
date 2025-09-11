package com.nttdata.accountservice.api;


import com.nttdata.accountservice.model.*;
import com.nttdata.accountservice.service.*;
import lombok.*;
import org.springframework.http.*;
import org.springframework.stereotype.*;
import org.springframework.web.server.*;
import reactor.core.publisher.*;
import lombok.extern.slf4j.Slf4j;
import java.net.*;

/**
 * Implementación del delegate de la API de cuentas.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountApiDelegateImpl implements ApiApiDelegate {

  private final AccountService service;

  @Override
  public Mono<ResponseEntity<AccountResponse>> registerAccount(
      Mono<AccountRequest> accountRequest, ServerWebExchange exchange) {
    log.info("Registrando nueva cuenta");
    return accountRequest
        .flatMap(service::createAccount)
        .map(resp -> ResponseEntity
            .created(URI.create("/api/accounts/" + resp.getId()))
            .body(resp));
  }

  @Override
  public Mono<ResponseEntity<InlineResponse200>> listAccounts(
      ServerWebExchange exchange) {
    log.info("Listando todas las cuentas");
    return service.listAccounts()
        .collectList()
        .map(list -> new InlineResponse200().items(list))
        .map(ResponseEntity::ok);
  }

  @Override
  public Mono<ResponseEntity<AccountResponse>> getAccountById(
      String id, ServerWebExchange exchange) {
    log.info("Obteniendo cuentas por id: {}", id);
    return service.getAccountById(id).map(ResponseEntity::ok);
  }

  @Override
  public Mono<ResponseEntity<AccountResponse>> updateAccount(
      String id, Mono<AccountRequest> accountRequest, ServerWebExchange exchange) {
    log.info("Actualizando cuenta por id: {}", id);
    return accountRequest
        .flatMap(req -> service.updateAccount(id, req))
        .map(ResponseEntity::ok);
  }

  @Override
  public Mono<ResponseEntity<Void>> deleteAccount(
      String id, ServerWebExchange exchange) {
    log.info("Eliminando cuenta por id: {}", id);
    return service.deleteAccount(id).thenReturn(ResponseEntity.noContent().build());

  }

  @Override
  public Mono<ResponseEntity<AccountLimitsResponse>> getAccountLimits(
      String id, ServerWebExchange exchange) {
    log.info("Obteniendo límites de cuenta por id: {}", id);
    return service.getAccountLimits(id)
        .map(ResponseEntity::ok);
  }

  @Override
  public Mono<ResponseEntity<BalanceOperationResponse>> applyBalanceOperation(
      String id, Mono<BalanceOperationRequest> balanceOperationRequest, ServerWebExchange exchange) {
    log.info("Aplicando operación de balance en cuenta por id: {}", id);
    return balanceOperationRequest
        .flatMap(req -> service.applyBalanceOperation(id, req))
        .map(ResponseEntity::ok);
  }

  @Override
  public Mono<ResponseEntity<Flux<AccountResponse>>> listAccountsByHolderDocument(
      String document, ServerWebExchange exchange) {
    log.info("obteneniendo cuentas por documento del titular: {}", document);
    return service.getAccountsByHolderDocument(document)
        .collectList()
        .map(list -> list.isEmpty()
            ? ResponseEntity.status(404).body(Flux.empty())
            : ResponseEntity.ok(Flux.fromIterable(list)));
  }
}
