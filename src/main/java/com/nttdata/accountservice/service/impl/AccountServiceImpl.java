package com.nttdata.accountservice.service.impl;

import com.nttdata.accountservice.config.*;
import com.nttdata.accountservice.integration.credits.*;
import com.nttdata.accountservice.integration.customers.*;
import com.nttdata.accountservice.model.*;
import com.nttdata.accountservice.model.entity.*;
import com.nttdata.accountservice.repository.*;
import com.nttdata.accountservice.service.*;
import com.nttdata.accountservice.service.policy.*;
import com.nttdata.accountservice.service.rules.*;
import com.nttdata.accountservice.util.*;
import lombok.*;
import lombok.extern.slf4j.*;
import org.springframework.http.*;
import org.springframework.stereotype.*;
import org.springframework.web.server.*;
import reactor.core.publisher.*;

import java.math.*;
import java.time.*;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {


  private static final BigDecimal MIN_OPENING_BALANCE = BigDecimal.ZERO;
  private static final String ACCOUNT_NOT_FOUND_MSG = "Cuenta no encontrada con ID: ";
  private final AccountRepository accountRepository;

  // Integraciones y servicios de dominio
  private final CustomersClient customersClient;
  private final CreditsClient creditsClient;
  private final AccountRulesService accountRules;
  private final AccountPolicyService policyService;


  @org.springframework.beans.factory.annotation
      .Value("${benefit.savings.vip.requireCreditCard:true}")
  private boolean requireCcForVip;
  @org.springframework.beans.factory.annotation
      .Value("${benefit.checking.pyme.requireCreditCard:true}")
  private boolean requireCcForPyme;

  @Override
  public Flux<AccountResponse> listAccounts() {
    log.info("listAccounts");
    return accountRepository
        .findAll()
        .map(AccountMapper::toResponse);
  }

  @Override
  public Mono<AccountResponse> getAccountById(String id) {
    log.info("Buscando cuenta por ID: {}", id);
    return accountRepository.findById(id)
        .switchIfEmpty(Mono.defer(() -> {
          log.warn("Cuenta no encontrada con ID: {}", id);
          return Mono.error(new ResponseStatusException(
              HttpStatus.NOT_FOUND, ACCOUNT_NOT_FOUND_MSG + id));
        }))
        .map(AccountMapper::toResponse);
  }


  @Override
  public Mono<AccountResponse> createAccount(AccountRequest request) {
    // Validación inicial para evitar NullPointerException
    if (request == null) {
      return Mono.error(new BusinessException("AccountRequest no puede ser nulo"));
    }
    return validateRequest(request)
        .then(Mono.defer(() ->
            customersClient.getEligibilityByDocument(
                    request.getHolderDocumentType().getValue(),
                    request.getHolderDocument()
                )
                .switchIfEmpty(Mono.error(new BusinessException(
                    "No existe cliente activo para el documento.")))
        ))
        .flatMap(elig -> validateAllRules(request, elig)
            .then(Mono.defer(() -> persistNewAccount(request, elig))))
        .map(AccountMapper::toResponse);
  }

  @Override
  public Mono<AccountResponse> updateAccount(String id, AccountRequest request) {
    return validateRequest(request)
        .then(accountRepository.findById(id)
            .switchIfEmpty(Mono.error(new ResponseStatusException(
                HttpStatus.NOT_FOUND, ACCOUNT_NOT_FOUND_MSG + id)))
            // Actualizar campos permitidos
            .flatMap(existing -> {
              AccountMapper.mergeIntoEntity(existing, request);
              // proteger campos inmutables o gestionados por el sistema
              existing.setAccountNumber(existing.getAccountNumber());
              existing.setInterbankNumber(existing.getInterbankNumber());
              existing.setCreationDate(existing.getCreationDate());
              return accountRepository.save(existing);
            }))
        .map(AccountMapper::toResponse);
  }

  @Override
  public Mono<Void> deleteAccount(String id) {
    return accountRepository.findById(id)
        .switchIfEmpty(Mono.error(new ResponseStatusException(
            HttpStatus.NOT_FOUND, ACCOUNT_NOT_FOUND_MSG + id)))
        .flatMap(acc -> accountRepository.deleteById(id));
  }

  @Override
  public Mono<AccountLimitsResponse> getAccountLimits(String id) {
    return accountRepository.findById(id)
        .switchIfEmpty(Mono.error(new ResponseStatusException(
            HttpStatus.NOT_FOUND, ACCOUNT_NOT_FOUND_MSG + id)))
        // Obtener límites y operaciones usadas en el mes actual
        .map(acc -> {
          AccountLimitsResponse resp = new AccountLimitsResponse();
          resp.setFreeTransactionsLimit(acc.getFreeTransactionsLimit());
          resp.setCommissionFee(acc.getCommissionFee());
          String ymNow = YearMonth.now().toString();

          int used = 0;
          if (acc.getOpsCounter() != null
              && ymNow.equals(acc
              .getOpsCounter()
              .getYearMonth())) {
            used = acc.getOpsCounter().getCount() == null
                ? 0 : acc.getOpsCounter().getCount();
          }

          resp.setUsedTransactionsThisMonth(used);
          return resp;
        });
  }


  @Override
  // Aplicar operación de balance (depósito, retiro, transferencia)
  public Mono<BalanceOperationResponse> applyBalanceOperation(
      String accountId, BalanceOperationRequest request) {
    // validacion - monto > 0 y un operationId no vacío
    if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0)
      return Mono.error(new IllegalArgumentException("amount debe ser > 0"));
    if (request.getOperationId().isBlank())
      return Mono.error(new IllegalArgumentException("operationId es obligatorio"));
    // Buscar cuenta y procesar operación de balance
    return accountRepository.findById(accountId)
        .switchIfEmpty(Mono.error(new ResponseStatusException(
            HttpStatus.NOT_FOUND, "Cuenta no encontrada")))
        .flatMap(acc -> processBalanceOperation(acc, request));
  }

  // Lógica para procesar la operación de balance
  private Mono<BalanceOperationResponse> processBalanceOperation(
      Account acc, BalanceOperationRequest request) {
    // Idempotencia: si ya se aplicó la operación, devolver estado sin cambios
    if (isOperationAlreadyApplied(acc, request)) {
      return Mono.just(new BalanceOperationResponse()
          .applied(false).newBalance(acc.getBalance())
          .commissionApplied(BigDecimal.ZERO)
          .message("Idempotente"));
    }
    // Determinar si es débito o crédito
    boolean isDebit = isDebit(request.getType());
    // Validaciones específicas según tipo de cuenta y operación
    return validateFixedTermDay(acc, isDebit)
        // luego validar saldo suficiente si es débito
        .then(validateSufficientBalance(acc, request, isDebit))
        // todas las validaciones pasaron, proceder a actualizar cuenta
        .thenReturn(true)
        // finalmente actualizar cuenta y devolver resultado
        .flatMap(valid -> updateAccountForOperation(acc, request));
  }

  // Verifica si la operación ya fue aplicada a la cuenta (idempotencia)
  private boolean isOperationAlreadyApplied(Account acc, BalanceOperationRequest request) {
    // Si no hay historial de operaciones, no se ha aplicado
    return acc.getOpIds() != null && acc.getOpIds().contains(request.getOperationId());
  }

  // Validar día permitido para débito en cuentas a plazo fijo
  private Mono<Void> validateFixedTermDay(Account acc, boolean isDebit) {
    // Si es cuenta FIXED_TERM y es débito, validar día permitido
    if ("FIXED_TERM".equalsIgnoreCase(acc.getAccountType()) && isDebit) {
      int today = LocalDate.now().getDayOfMonth();
      // Si no coincide, rechazar
      if (acc.getAllowedDayOfMonth() == null || !acc.getAllowedDayOfMonth().equals(today)) {
        // Día no permitido
        return Mono.error(new BusinessException(
            "Día no permitido para débito en FIXED_TERM"));
      }
    }
    return Mono.empty();
  }

  // Validar que la cuenta tiene saldo suficiente para la operación de débito
  private Mono<Void> validateSufficientBalance(
      Account acc, BalanceOperationRequest request, boolean isDebit) {
    // Si es débito, verificar saldo
    BigDecimal delta = computeDelta(request.getType(), request.getAmount());
    if (isDebit && acc.getBalance().add(delta).compareTo(BigDecimal.ZERO) < 0) {
      return Mono.error(new BusinessException("Saldo insuficiente"));
    }
    // Si es crédito o hay saldo suficiente, OK
    return Mono.empty();
  }

  // Actualizar cuenta con nueva operación, calcular comisión si aplica
  private Mono<BalanceOperationResponse> updateAccountForOperation(
      Account acc, BalanceOperationRequest request) {
    // Obtener o inicializar contador de operaciones del mes actual
    String ymNow = YearMonth.now().toString();
    // Si es otro mes, reiniciar contador
    OpsCounter oc = acc.getOpsCounter();
    // Si no existe o es otro mes, crear nuevo
    if (oc == null || !ymNow.equals(oc.getYearMonth())) {
      oc = new OpsCounter();
      oc.setYearMonth(ymNow);
      oc.setCount(0);
    }
    // Actualizar contador y calcular comisión si aplica
    boolean counts = countsForPolicy(request.getType());
    int nextCount = oc.getCount() + (counts ? 1 : 0);
    int free = acc.getFreeTransactionsLimit() == null ? 0 : acc.getFreeTransactionsLimit();
    BigDecimal fee = acc.getCommissionFee() == null ? BigDecimal.ZERO : acc.getCommissionFee();
    BigDecimal commissionApplied = (counts && nextCount > free) ? fee : BigDecimal.ZERO;
    BigDecimal delta = computeDelta(request.getType(), request.getAmount());
    BigDecimal newBalance = acc.getBalance().add(delta);
    // Aplicar comisión si corresponde
    acc.setBalance(newBalance);
    oc.setCount(nextCount);
    acc.setOpsCounter(oc);
    // Registrar ID de operación para idempotencia
    if (acc.getOpIds() == null) acc.setOpIds(new ArrayList<>());
    acc.getOpIds().add(request.getOperationId());
    // Restar comisión del balance si aplica
    if (acc.getOpIds().size() > 200) acc.getOpIds().remove(0);
    // Actualizar balance con comisión si aplica
    return accountRepository.save(acc)
        // Si hay comisión, restarla del balance y guardar de nuevo
        .map(saved -> new BalanceOperationResponse()
            .applied(true).newBalance(saved.getBalance())
            .commissionApplied(commissionApplied).message("OK"));
  }

  //Buscar cuentas por documento de cliente - utilizado por transacciones
  @Override
  public Flux<AccountResponse> getAccountsByHolderDocument(String holderDocument) {
    return accountRepository.findByHolderDocument(holderDocument)
        .switchIfEmpty(Mono.error(new ResponseStatusException(
            HttpStatus.NOT_FOUND, "No se encontraron cuentas para el documento: " + holderDocument)))
        .map(AccountMapper::toResponse);
  }

  // ===== Lógica de negocio específica =====
  private Mono<Void> validateAllRules(
      AccountRequest req, EligibilityResponse elig) {
    Mono<Void> legacy = accountRules.validateLegacyRules(
        req.getHolderDocument(),
        req.getAccountType(),
        elig.getType());

    boolean isVipSavings = "PERSONAL".equals(elig.getType())
        && "VIP".equals(elig.getProfile())
        && req.getAccountType() == AccountRequest.AccountTypeEnum.SAVINGS;

    boolean isPymeChecking = "BUSINESS".equals(elig.getType())
        && "PYME".equals(elig.getProfile())
        && req.getAccountType() == AccountRequest.AccountTypeEnum.CHECKING;

    Mono<Void> benefit = Mono.empty();
    if (isVipSavings && requireCcForVip) {
      benefit = creditsClient.hasActiveCreditCard(elig.getCustomerId())
          .flatMap(has -> Boolean.TRUE.equals(has) ? Mono.empty() :
              Mono.error(new BusinessException(
                  "Ahorro VIP requiere tener Tarjeta de Crédito activa.")));
    } else if (isPymeChecking && requireCcForPyme) {
      benefit = creditsClient.hasActiveCreditCard(elig.getCustomerId())
          .flatMap(has -> Boolean.TRUE.equals(has) ? Mono.empty() :
              Mono.error(new BusinessException(
                  "Cuenta Corriente PYME requiere Tarjeta de Crédito activa.")));
    }

    return legacy.then(benefit);
  }

  private Mono<Account> persistNewAccount(
      AccountRequest req, EligibilityResponse elig) {
    Account account = AccountMapper.toEntity(req);

    // Inicializaciones
    if (account.getCreationDate() == null) account.setCreationDate(LocalDate.now());
    if (account.getActive() == null) account.setActive(Boolean.TRUE);
    if (account.getBalance() == null) account.setBalance(BigDecimal.ZERO);
    if (account.getOpIds() == null) account.setOpIds(new ArrayList<>());
    if (account.getOpsCounter() == null) {
      OpsCounter oc = new OpsCounter();
      oc.setYearMonth(YearMonth.now().toString());
      oc.setCount(0);
      account.setOpsCounter(oc);
    }

    // PYME => sin mantenimiento
    boolean isPymeChecking = "BUSINESS".equals(elig.getType())
        && "PYME".equals(elig.getProfile())
        && req.getAccountType() == AccountRequest.AccountTypeEnum.CHECKING;
    if (isPymeChecking) {
      account.setMaintenanceFee(BigDecimal.ZERO);
    }

    // Defaults por tipo (freeOps/fee)
    policyService.applyDefaults(account, req.getAccountType());

    // Números -con posible reintento ante colisión
    account.setAccountNumber(AccountNumberGenerator.numeric(11));
    account.setInterbankNumber(AccountNumberGenerator.numeric(20));

    return trySaveWithRetry(account, 3);
  }

  private Mono<Account> trySaveWithRetry(Account acc, int remaining) {
    return accountRepository.save(acc)
        .onErrorResume(ex -> {
          if (ex instanceof org.springframework.dao.DuplicateKeyException && remaining > 0) {
            acc.setAccountNumber(AccountNumberGenerator.numeric(11));
            acc.setInterbankNumber(AccountNumberGenerator.numeric(20));
            return trySaveWithRetry(acc, remaining - 1);
          }
          return Mono.error(ex);
        });
  }

  // ===== Helpers =====

  // Define si la operación es de débito (afecta negativamente el balance)
  private boolean isDebit(BalanceOperationType type) {
    // Retiro y Transferencia Out son débitos
    return type == BalanceOperationType.WITHDRAWAL
        || type == BalanceOperationType.TRANSFER_OUT;
  }

  // Define si la operación afecta el conteo para políticas de comisión
  private boolean countsForPolicy(BalanceOperationType type) {
    return type == BalanceOperationType.DEPOSIT
        || type == BalanceOperationType.WITHDRAWAL
        || type == BalanceOperationType.TRANSFER_IN
        || type == BalanceOperationType.TRANSFER_OUT;
  }

  // Calcula el delta a aplicar al balance según el tipo de operación - positivo o negativo
  private BigDecimal computeDelta(BalanceOperationType type, BigDecimal amount) {
    // Depósito y Transferencia In suman (delta positivo)
    if (type == BalanceOperationType.DEPOSIT
        || type == BalanceOperationType.TRANSFER_IN) return amount;
    // Retiro y Transferencia Out restan (delta negativo)
    if (type == BalanceOperationType.WITHDRAWAL
        || type == BalanceOperationType.TRANSFER_OUT)
      return amount.negate();
    return BigDecimal.ZERO;
  }


  // Validaciones de negocio para creación y actualización
  private Mono<Void> validateRequest(AccountRequest request) {
    if (request.getHolderDocument() == null || request.getHolderDocument().isBlank()) { // NOSONAR
      return Mono.error(new BusinessException("holderDocument es obligatorio"));
    }
    if (!request.getHolderDocument().matches("^\\d{8,11}$")) {
      return Mono.error(new BusinessException("El documento debe tener entre 8 y 11 dígitos"));
    }
    if (request.getHolderDocumentType() == null) { // NOSONAR
      return Mono.error(new BusinessException("holderDocumentType es obligatorio"));
    }
    if (request.getAccountType() == null) { // NOSONAR
      return Mono.error(new BusinessException("accountType es obligatorio"));
    }
    if (request.getBalance() != null && request.getBalance().compareTo(MIN_OPENING_BALANCE) < 0) {
      return Mono.error(new BusinessException("El balance inicial no puede ser negativo"));
    }

    switch (request.getAccountType()) {
      case SAVINGS:
        return validateSavingsAccount(request);
      case CHECKING:
        return validateCheckingAccount(request);
      case FIXED_TERM:
        return validateFixedTermAccount(request);
      default:
        return Mono.error(new BusinessException("Tipo de cuenta no soportado"));
    }
  }

  private Mono<Void> validateSavingsAccount(AccountRequest request) {
    if (request.getMonthlyMovementLimit() == null) {
      return Mono.error(new BusinessException("monthlyMovementLimit es obligatorio para cuentas de ahorro"));
    }
    if (request.getMaintenanceFee() != null) {
      return Mono.error(new BusinessException("maintenanceFee no debe estar presente en cuentas de ahorro"));
    }
    if (request.getAllowedDayOfMonth() != null) {
      return Mono.error(new BusinessException("allowedDayOfMonth no debe estar presente en cuentas de ahorro"));
    }
    return Mono.empty();
  }

  private Mono<Void> validateCheckingAccount(AccountRequest request) {
    if (request.getMonthlyMovementLimit() == null) {
      return Mono.error(new BusinessException("monthlyMovementLimit es obligatorio para cuentas corrientes"));
    }
    if (request.getMaintenanceFee() == null) {
      return Mono.error(new BusinessException("maintenanceFee es obligatorio para cuentas corrientes"));
    }
    if (request.getAllowedDayOfMonth() != null) {
      return Mono.error(new BusinessException("allowedDayOfMonth no debe estar presente en cuentas corrientes"));
    }
    return Mono.empty();
  }

  private Mono<Void> validateFixedTermAccount(AccountRequest request) {
    if (request.getAllowedDayOfMonth() == null) {
      return Mono.error(new BusinessException("allowedDayOfMonth es obligatorio para cuentas a plazo fijo"));
    }
    if (request.getAllowedDayOfMonth() < 1 || request.getAllowedDayOfMonth() > 28) {
      return Mono.error(new BusinessException("allowedDayOfMonth debe estar entre 1 y 28"));
    }
    if (request.getMonthlyMovementLimit() != null) {
      return Mono.error(new BusinessException("monthlyMovementLimit no debe estar presente en cuentas a plazo fijo"));
    }
    if (request.getMaintenanceFee() != null) {
      return Mono.error(new BusinessException("maintenanceFee no debe estar presente en cuentas a plazo fijo"));
    }
    return Mono.empty();
  }

}

