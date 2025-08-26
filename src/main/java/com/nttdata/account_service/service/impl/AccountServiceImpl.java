package com.nttdata.account_service.service.impl;

import com.nttdata.account_service.model.AccountRequest;
import com.nttdata.account_service.model.AccountResponse;
import com.nttdata.account_service.model.entity.Account;
import com.nttdata.account_service.repository.AccountRepository;
import com.nttdata.account_service.service.AccountMapper;
import com.nttdata.account_service.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.security.SecureRandom;


@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountServiceImpl.class);
    private final AccountRepository accountRepository;

    @Override
    public Flux<AccountResponse> listAccounts() {
        log.debug("Listando todas las cuentas bancarias");
        return accountRepository.findAll()
                .map(AccountMapper::toResponse)
                .doOnComplete(() -> log.info("Listado de cuentas completado"));
    }

    @Override
    public Mono<AccountResponse> getAccountById(String id) {
        log.debug("Buscando cuenta con ID: {}", id);
        return accountRepository.findById(id)
                .map(AccountMapper::toResponse)
                .doOnNext(acc -> log.info("Cuenta encontrada: {}", acc.getAccountNumber()))
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Cuenta no encontrada con ID: " + id)));
    }

    @Override
    public Mono<AccountResponse> createAccount(AccountRequest request) {
        log.info("Creando nueva cuenta para documento: {}", request.getHolderDocument());

        return validateRequest(request)
                .then(Mono.defer(() -> {
                    Account account = AccountMapper.toEntity(request);

                    // Autogenerar números si no existen
                    if (account.getAccountNumber() == null || account.getAccountNumber().isBlank()) {
                        account.setAccountNumber(generateAccountNumber(11));
                    }
                    if (account.getInterbankNumber() == null || account.getInterbankNumber().isBlank()) {
                        account.setInterbankNumber(generateAccountNumber(20));
                    }

                    return accountRepository.save(account);
                }))
                .map(AccountMapper::toResponse)
                .doOnSuccess(acc -> log.debug("Cuenta creada con número: {}", acc.getAccountNumber()));
    }

    @Override
    public Mono<AccountResponse> updateAccount(String id, AccountRequest request) {
        log.info("Actualizando cuenta con ID: {}", id);

        return validateRequest(request)
                .then(accountRepository.findById(id)
                        .switchIfEmpty(Mono.error(new IllegalArgumentException("Cuenta no encontrada con ID: " + id)))
                        .flatMap(existing -> {
                            Account account = AccountMapper.toEntity(request);
                            account.setId(existing.getId());

                            // Mantener números autogenerados si no vienen en el request
                            if (account.getAccountNumber() == null || account.getAccountNumber().isBlank()) {
                                account.setAccountNumber(existing.getAccountNumber());
                            }
                            if (account.getInterbankNumber() == null || account.getInterbankNumber().isBlank()) {
                                account.setInterbankNumber(existing.getInterbankNumber());
                            }

                            return accountRepository.save(account);
                        })
                        .map(AccountMapper::toResponse)
                        .doOnSuccess(acc -> log.debug("Cuenta actualizada: {}", acc.getAccountNumber())));
    }

    @Override
    public Mono<Void> deleteAccount(String id) {
        log.info("Solicitud de eliminación de cuenta con ID: {}", id);
        return accountRepository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Cuenta no encontrada con ID: " + id)))
                .flatMap(account -> {
                    log.debug("Cuenta eliminada: {}", account.getAccountNumber());
                    return accountRepository.deleteById(id);
                });
    }

    /**
     * Validaciones de negocio (ahora reactivas)
     */
    private Mono<Void> validateRequest(AccountRequest request) {
        if (request.getHolderDocument() == null || request.getHolderDocument().isBlank()) {
            return Mono.error(new IllegalArgumentException("El documento del titular es obligatorio"));
        }
        if (request.getBalance() != null && request.getBalance().compareTo(BigDecimal.ZERO) < 0) {
            return Mono.error(new IllegalArgumentException("El saldo inicial no puede ser negativo"));
        }
        if (request.getAccountType() == null) {
            return Mono.error(new IllegalArgumentException("El tipo de cuenta es obligatorio"));
        }

        switch (request.getAccountType()) {
            case SAVINGS:
                if (request.getMonthlyMovementLimit() == null) {
                    return Mono.error(new IllegalArgumentException(
                            "El límite mensual de movimientos es obligatorio para cuentas de ahorro (SAVINGS)"
                    ));
                }
                break;
            case CHECKING:
                if (request.getMaintenanceFee() == null) {
                    return Mono.error(new IllegalArgumentException(
                            "La comisión de mantenimiento es obligatoria para cuentas corrientes (CHECKING)"
                    ));
                }
                break;
            case FIXED_TERM:
                if (request.getAllowedDayOfMonth() == null) {
                    return Mono.error(new IllegalArgumentException(
                            "El día permitido de movimiento es obligatorio para cuentas a plazo fijo (FIXED_TERM)"
                    ));
                }
                break;
            default:
                return Mono.error(new IllegalArgumentException("Tipo de cuenta no soportado"));
        }
        return Mono.empty();
    }

    /**
     * Generador de números de cuenta/interbancario
     */
    private String generateAccountNumber(int length) {
        SecureRandom random = new SecureRandom();
        return random.ints(length, 0, 10)
                .mapToObj(String::valueOf)
                .reduce("", String::concat);
    }
}
