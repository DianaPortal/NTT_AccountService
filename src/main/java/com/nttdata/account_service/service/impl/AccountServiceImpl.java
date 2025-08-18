package com.nttdata.account_service.service.impl;

import com.nttdata.account_service.model.AccountRequest;
import com.nttdata.account_service.model.AccountResponse;
import com.nttdata.account_service.model.entity.Account;
import com.nttdata.account_service.service.AccountMapper;
import com.nttdata.account_service.repository.AccountRepository;
import com.nttdata.account_service.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountServiceImpl.class);

    private final AccountRepository accountRepository;

    @Override
    public ResponseEntity<List<AccountResponse>> listAccounts() {
        log.debug("Listando todas las cuentas bancarias");
        List<AccountResponse> responses = accountRepository.findAll()
                .stream()
                .map(AccountMapper::toResponse)
                .collect(Collectors.toList());
        log.info("Se encontraron {} cuentas bancarias", responses.size());
        return ResponseEntity.ok(responses);
    }

    @Override
    public ResponseEntity<AccountResponse> getAccountById(String id) {
        log.debug("Buscando cuenta con ID: {}", id);
        return accountRepository.findById(id)
                .map(account -> {
                    log.info("Cuenta encontrada: {}", account.getAccountNumber());
                    return ResponseEntity.ok(AccountMapper.toResponse(account));
                })
                .orElseThrow(() -> {
                    log.warn("Cuenta no encontrada con ID: {}", id);
                    return new IllegalArgumentException("Cuenta no encontrada con ID: " + id);
                });
    }

    @Override
    public ResponseEntity<AccountResponse> createAccount(AccountRequest request) {
        log.info("Creando nueva cuenta para documento: {}", request.getHolderDocument());

        validateRequest(request);

        Account account = AccountMapper.toEntity(request);

        // Autogenerar números si no existen
        if (account.getAccountNumber() == null || account.getAccountNumber().isBlank()) {
            account.setAccountNumber(generateAccountNumber(11));
        }
        if (account.getInterbankNumber() == null || account.getInterbankNumber().isBlank()) {
            account.setInterbankNumber(generateAccountNumber(20));
        }

        Account saved = accountRepository.save(account);
        log.debug("Cuenta creada con número: {}", saved.getAccountNumber());
        return ResponseEntity.ok(AccountMapper.toResponse(saved));
    }

    @Override
    public ResponseEntity<AccountResponse> updateAccount(String id, AccountRequest request) {
        log.info("Actualizando cuenta con ID: {}", id);

        validateRequest(request);

        return accountRepository.findById(id)
                .map(existing -> {
                    Account account = AccountMapper.toEntity(request);
                    account.setId(existing.getId());

                    // Mantener números autogenerados si no vienen en el request
                    if (account.getAccountNumber() == null || account.getAccountNumber().isBlank()) {
                        account.setAccountNumber(existing.getAccountNumber());
                    }
                    if (account.getInterbankNumber() == null || account.getInterbankNumber().isBlank()) {
                        account.setInterbankNumber(existing.getInterbankNumber());
                    }

                    Account updated = accountRepository.save(account);
                    log.debug("Cuenta actualizada: {}", updated.getAccountNumber());
                    return ResponseEntity.ok(AccountMapper.toResponse(updated));
                })
                .orElseThrow(() -> {
                    log.warn("No se encontró cuenta para actualizar con ID: {}", id);
                    return new IllegalArgumentException("Cuenta no encontrada con ID: " + id);
                });
    }

    @Override
    public ResponseEntity<Void> deleteAccount(String id) {
        log.info("Solicitud de eliminación de cuenta con ID: {}", id);
        return accountRepository.findById(id)
                .map(account -> {
                    accountRepository.deleteById(id);
                    log.debug("Cuenta eliminada: {}", account.getAccountNumber());
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElseThrow(() -> {
                    log.warn("No se encontró cuenta para eliminar con ID: {}", id);
                    return new IllegalArgumentException("Cuenta no encontrada con ID: " + id);
                });
    }

    /**
     * Validaciones de negocio
     */
    private void validateRequest(AccountRequest request) {
        if (request.getHolderDocument() == null || request.getHolderDocument().isBlank()) {
            throw new IllegalArgumentException("El documento del titular es obligatorio");
        }
        if (request.getBalance() != null && request.getBalance().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El saldo inicial no puede ser negativo");
        }
        if (request.getAccountType() == null) {
            throw new IllegalArgumentException("El tipo de cuenta es obligatorio");
        }

        switch (request.getAccountType()) {
            case SAVINGS:
                if (Objects.isNull(request.getMonthlyMovementLimit())) {
                    throw new IllegalArgumentException("El límite mensual de movimientos es obligatorio para cuentas de ahorro (SAVINGS)");
                }
                break;
            case CHECKING:
                if (Objects.isNull(request.getMaintenanceFee())) {
                    throw new IllegalArgumentException("La comisión de mantenimiento es obligatoria para cuentas corrientes (CHECKING)");
                }
                break;
            case FIXED_TERM:
                if (Objects.isNull(request.getAllowedDayOfMonth())) {
                    throw new IllegalArgumentException("El día permitido de movimiento es obligatorio para cuentas a plazo fijo (FIXED_TERM)");
                }
                break;
            default:
                throw new IllegalArgumentException("Tipo de cuenta no soportado: " + request.getAccountType());
        }
    }

    /**
     * Generador de números de cuenta/interbancario
     */
    private String generateAccountNumber(int length) {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10)); // solo dígitos
        }
        return sb.toString();
    }
}
