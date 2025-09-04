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
import org.slf4j.*;
import org.springframework.http.*;
import org.springframework.stereotype.*;
import org.springframework.web.server.*;
import reactor.core.publisher.*;

import java.math.*;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

  private static final Logger log = LoggerFactory.getLogger(AccountServiceImpl.class);
  private static final BigDecimal MIN_OPENING_BALANCE = BigDecimal.ZERO;

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
    return accountRepository.findAll().map(AccountMapper::toResponse);
  }

  @Override
  public Mono<AccountResponse> getAccountById(String id) {
    return accountRepository.findById(id)
        .switchIfEmpty(Mono.error(new ResponseStatusException(
            HttpStatus.NOT_FOUND, "Cuenta no encontrada con ID: " + id)))
        .map(AccountMapper::toResponse);
  }


  @Override
  public Mono<AccountResponse> createAccount(AccountRequest request) {
    // Validación inicial para evitar NullPointerException
    if (request == null) {
      return Mono.error(new BusinessException("AccountRequest no puede ser nulo"));
    }

    if (request.getHolderDocument() == null || request.getHolderDocument().isBlank()) {
      return Mono.error(new BusinessException("holderDocument es obligatorio"));
    }
    if (request.getAccountType() == null) {
      return Mono.error(new BusinessException("accountType es obligatorio"));
    }
    if (request.getHolderDocumentType() == null) {
      return Mono.error(new BusinessException("holderDocumentType es obligatorio"));
    }
    if (!request.getHolderDocument().matches("\\d{8,11}")) {
      return Mono.error(new BusinessException("holderDocument debe tener entre 8 y 11 dígitos"));
    }
    if (request.getBalance() != null && request.getBalance().compareTo(BigDecimal.ZERO) < 0) {
      return Mono.error(new BusinessException("El balance inicial no puede ser negativo"));
    }
    // Validaciones específicas por tipo de cuenta
    switch (request.getAccountType()) {
      case SAVINGS:
        if (request.getMonthlyMovementLimit() == null) {
          return Mono.error(new BusinessException("monthlyMovementLimit es obligatorio para cuentas de ahorro"));
        }
        /*if (request.getAllowedDayOfMonth() == null) {
          return Mono.error(new BusinessException("allowedDayOfMonth es obligatorio para cuentas de ahorro"));
        }*/
        break;

      case CHECKING:
        if (request.getMonthlyMovementLimit() == null) {
          return Mono.error(new BusinessException("monthlyMovementLimit es obligatorio para cuentas corrientes"));
        }
        if (request.getMaintenanceFee() == null) {
          return Mono.error(new BusinessException("maintenanceFee es obligatorio para cuentas corrientes"));
        }
        break;

      case FIXED_TERM:
        if (request.getAllowedDayOfMonth() == null) {
          return Mono.error(new BusinessException("allowedDayOfMonth es obligatorio para cuentas a plazo fijo"));
        }
        if (request.getMonthlyMovementLimit() != null) {
          return Mono.error(new BusinessException("monthlyMovementLimit no debe estar presente en cuentas a plazo fijo"));
        }
        if (request.getMaintenanceFee() != null) {
          return Mono.error(new BusinessException("maintenanceFee no debe estar presente en cuentas a plazo fijo"));
        }
        break;
    }
    return validateRequest(request)
        .then(
            customersClient.getEligibilityByDocument(
                    request.getHolderDocumentType().getValue(),  // o .name()
                    request.getHolderDocument()
                )
                .switchIfEmpty(Mono.error(new BusinessException(
                    "No existe cliente activo para el documento.")))
        )
        .flatMap(elig -> validateAllRules(request, elig)
            .then(Mono.defer(() -> persistNewAccount(request, elig))))
        .map(AccountMapper::toResponse);
  }


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
          .flatMap(has -> has ? Mono.empty() :
              Mono.error(new BusinessException(
                  "Ahorro VIP requiere tener Tarjeta de Crédito activa.")));
    } else if (isPymeChecking && requireCcForPyme) {
      benefit = creditsClient.hasActiveCreditCard(elig.getCustomerId())
          .flatMap(has -> has ? Mono.empty() :
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

  @Override
  public Mono<AccountResponse> updateAccount(String id, AccountRequest request) {
    return validateRequest(request)
        .then(accountRepository.findById(id)
            .switchIfEmpty(Mono.error(new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Cuenta no encontrada con ID: " + id)))
            .flatMap(existing -> {
              AccountMapper.mergeIntoEntity(existing, request);
              // proteger campos inmutables
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
            HttpStatus.NOT_FOUND, "Cuenta no encontrada con ID: " + id)))
        .flatMap(acc -> accountRepository.deleteById(id));
  }

  @Override
  public Mono<AccountLimitsResponse> getAccountLimits(String id) {
    return accountRepository.findById(id)
        .switchIfEmpty(Mono.error(new ResponseStatusException(
            HttpStatus.NOT_FOUND, "Cuenta no encontrada con ID: " + id)))
        .map(acc -> {
          AccountLimitsResponse resp = new AccountLimitsResponse();
          resp.setFreeTransactionsLimit(acc.getFreeTransactionsLimit());
          resp.setCommissionFee(acc.getCommissionFee());
          String ymNow = YearMonth.now().toString();
          int used = (acc.getOpsCounter() != null
              && ymNow.equals(acc.getOpsCounter().getYearMonth()))
              ? (acc.getOpsCounter().getCount() == null ? 0 :
              acc.getOpsCounter().getCount()) : 0;
          resp.setUsedTransactionsThisMonth(used);
          return resp;
        });
  }

  @Override
  public Mono<BalanceOperationResponse> applyBalanceOperation(
      String accountId, BalanceOperationRequest request) {
    if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0)
      return Mono.error(new IllegalArgumentException("amount debe ser > 0"));
    if (request.getOperationId() == null || request.getOperationId().isBlank())
      return Mono.error(new IllegalArgumentException("operationId es obligatorio"));

    return accountRepository.findById(accountId)
        .switchIfEmpty(Mono.error(new ResponseStatusException(
            HttpStatus.NOT_FOUND, "Cuenta no encontrada")))
        .flatMap(acc -> {
          if (acc.getOpIds() != null && acc.getOpIds().contains(request.getOperationId())) {
            return Mono.just(new BalanceOperationResponse()
                .applied(false).newBalance(acc.getBalance())
                .commissionApplied(BigDecimal.ZERO).message("Idempotente"));
          }

          boolean isDebit = isDebit(request.getType());
          if ("FIXED_TERM".equalsIgnoreCase(acc.getAccountType()) && isDebit) {
            int today = LocalDate.now().getDayOfMonth();
            if (acc.getAllowedDayOfMonth() == null || !acc.getAllowedDayOfMonth().equals(today))
              return Mono.error(new BusinessException(
                  "Día no permitido para débito en FIXED_TERM"));
          }

          BigDecimal delta = computeDelta(request.getType(), request.getAmount());
          if (isDebit && acc.getBalance().add(delta).compareTo(BigDecimal.ZERO) < 0)
            return Mono.error(new BusinessException("Saldo insuficiente"));

          String ymNow = YearMonth.now().toString();
          OpsCounter oc = acc.getOpsCounter();
          if (oc == null || !ymNow.equals(oc.getYearMonth())) {
            oc = new OpsCounter();
            oc.setYearMonth(ymNow);
            oc.setCount(0);
          }
          boolean counts = countsForPolicy(request.getType());
          int nextCount = oc.getCount() + (counts ? 1 : 0);

          int free = acc.getFreeTransactionsLimit() == null ? 0 : acc.getFreeTransactionsLimit();
          BigDecimal fee = acc.getCommissionFee() == null ? BigDecimal.ZERO :
              acc.getCommissionFee();
          BigDecimal commissionApplied = (counts && nextCount > free) ? fee :
              BigDecimal.ZERO;

          BigDecimal newBalance = acc.getBalance().add(delta);

          acc.setBalance(newBalance);
          oc.setCount(nextCount);
          acc.setOpsCounter(oc);
          if (acc.getOpIds() == null) acc.setOpIds(new ArrayList<>());
          acc.getOpIds().add(request.getOperationId());
          if (acc.getOpIds().size() > 200) acc.getOpIds().remove(0);

          return accountRepository.save(acc)
              .map(saved -> new BalanceOperationResponse()
                  .applied(true).newBalance(saved.getBalance())
                  .commissionApplied(commissionApplied).message("OK"));
        });
  }

  @Override
  public Flux<AccountResponse> getAccountsByHolderDocument(String holderDocument) {
    return accountRepository.findByHolderDocument(holderDocument)
        .map(AccountMapper::toResponse);
  }

  // ===== Helpers =====

  private boolean isDebit(BalanceOperationType type) {
    return type == BalanceOperationType.WITHDRAWAL || type == BalanceOperationType.TRANSFER_OUT;
  }

  private boolean countsForPolicy(BalanceOperationType type) {
    return type == BalanceOperationType.DEPOSIT
        || type == BalanceOperationType.WITHDRAWAL
        || type == BalanceOperationType.TRANSFER_IN
        || type == BalanceOperationType.TRANSFER_OUT;
  }

  private BigDecimal computeDelta(BalanceOperationType type, BigDecimal amount) {
    if (type == BalanceOperationType.DEPOSIT
        || type == BalanceOperationType.TRANSFER_IN) return amount;
    if (type == BalanceOperationType.WITHDRAWAL
        || type == BalanceOperationType.TRANSFER_OUT)
      return amount.negate();
    return BigDecimal.ZERO;
  }

  private Mono<Void> validateRequest(AccountRequest request) {
    if (request.getHolderDocument() == null || request.getHolderDocument().isBlank())
      return Mono.error(new IllegalArgumentException(
          "holderDocument es obligatorio"));
    if (!request.getHolderDocument().matches("^\\d{8,11}$"))
      return Mono.error(new IllegalArgumentException(
          "El documento debe tener entre 8 y 11 dígitos"));
    if (request.getAccountType() == null)
      return Mono.error(new IllegalArgumentException(
          "accountType es obligatorio"));
    if (request.getBalance() != null && request.getBalance().compareTo(MIN_OPENING_BALANCE) < 0)
      return Mono.error(new IllegalArgumentException(
          "El saldo inicial no puede ser menor a 0"));

    switch (request.getAccountType()) {
      case SAVINGS:
        if (request.getMonthlyMovementLimit() == null)
          return Mono.error(new IllegalArgumentException(
              "monthlyMovementLimit es obligatorio para SAVINGS"));
        if (request.getMaintenanceFee() != null)
          return Mono.error(new IllegalArgumentException(
              "maintenanceFee no aplica a SAVINGS"));
        if (request.getAllowedDayOfMonth() != null)
          return Mono.error(new IllegalArgumentException(
              "allowedDayOfMonth no aplica a SAVINGS"));
        break;
      case CHECKING:
        if (request.getMaintenanceFee() == null)
          return Mono.error(new IllegalArgumentException(
              "maintenanceFee es obligatorio para CHECKING"));
        if (request.getMonthlyMovementLimit() != null)
          return Mono.error(new IllegalArgumentException(
              "monthlyMovementLimit no aplica a CHECKING"));
        if (request.getAllowedDayOfMonth() != null)
          return Mono.error(new IllegalArgumentException(
              "allowedDayOfMonth no aplica a CHECKING"));
        break;
      case FIXED_TERM:
        if (request.getAllowedDayOfMonth() == null)
          return Mono.error(new IllegalArgumentException(
              "allowedDayOfMonth es obligatorio para FIXED_TERM"));
        if (request.getAllowedDayOfMonth() < 1 || request.getAllowedDayOfMonth() > 28)
          return Mono.error(new IllegalArgumentException(
              "allowedDayOfMonth debe estar entre 1 y 28"));
        if (request.getMonthlyMovementLimit() != null)
          return Mono.error(new IllegalArgumentException(
              "monthlyMovementLimit no aplica a FIXED_TERM"));
        if (request.getMaintenanceFee() != null)
          return Mono.error(new IllegalArgumentException(
              "maintenanceFee no aplica a FIXED_TERM"));
        break;
      default:
        return Mono.error(new IllegalArgumentException(
            "Tipo de cuenta no soportado"));
    }
    return Mono.empty();
  }
}

