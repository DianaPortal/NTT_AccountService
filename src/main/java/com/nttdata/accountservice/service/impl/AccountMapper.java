package com.nttdata.accountservice.service.impl;

import com.nttdata.accountservice.model.*;
import com.nttdata.accountservice.model.entity.LinkedCard;
import com.nttdata.accountservice.model.entity.*;
import org.openapitools.jackson.nullable.*;

import java.math.*;
import java.util.*;
import java.util.function.*;
import java.util.regex.*;
import java.util.stream.*;

import static java.util.Optional.*;


public class AccountMapper {


  private static final Pattern DOC_PATTERN = Pattern.compile("^\\d{8,11}$");

  private AccountMapper() {
  }

  public static AccountResponse toResponse(Account account) {
    if (account == null) return null;

    AccountResponse dto = new AccountResponse();

    dto.setId(account.getId());
    dto.setAccountNumber(account.getAccountNumber());
    dto.setInterbankNumber(account.getInterbankNumber());

    dto.setHolderDocument(safeTrim(account.getHolderDocument()));
    dto.setHolderDocumentType(
        Optional.ofNullable(account.getHolderDocumentType())
            .map(String::trim)
            .flatMap(t -> {
              try {
                return Optional.of(AccountResponse.HolderDocumentTypeEnum.valueOf(t));
              } catch (IllegalArgumentException ex) {
                return Optional.empty();
              }
            }).orElse(null)
    );
    // Firmantes
    dto.setAuthorizedSigners(cleanDocs(account.getAuthorizedSigners()));

    // accountType
    dto.setAccountType(toResponseEnum(account.getAccountType()));

    dto.setBalance(ofNullable(account.getBalance()).orElse(BigDecimal.ZERO));
    dto.setInterestRate(account.getInterestRate());
    dto.setMonthlyMovementLimit(jn(account.getMonthlyMovementLimit()));
    dto.setMaintenanceFee(jn(account.getMaintenanceFee()));
    dto.setAllowedDayOfMonth(jn(account.getAllowedDayOfMonth()));
    dto.setCreationDate(account.getCreationDate());
    dto.setActive(account.getActive());

    if (account.getLinkedCard() != null) {
      var lc = new com.nttdata.accountservice.model.LinkedCard();
      lc.setId(account.getLinkedCard().getId());
      dto.setLinkedCard(lc);
    }

    // Política por cuenta
    dto.setFreeTransactionsLimit(account.getFreeTransactionsLimit());
    dto.setCommissionFee(account.getCommissionFee());

    return dto;
  }


  public static Account toEntity(AccountRequest request) {
    if (request == null) return null;

    Account acc = new Account();


    acc.setHolderDocument(safeTrim(request.getHolderDocument()));
    acc.setHolderDocumentType(
        Optional.ofNullable(request.getHolderDocumentType())
            .map(Enum::name)
            .orElse(null));
    acc.setAuthorizedSigners(cleanDocs(request.getAuthorizedSigners()));

    // Guardamos como String en entity (SAVINGS|CHECKING|FIXED_TERM)
    acc.setAccountType(ofNullable(request.getAccountType()).map(Enum::name).orElse(null));

    acc.setBalance(request.getBalance());
    acc.setInterestRate(request.getInterestRate());
    acc.setMonthlyMovementLimit(request.getMonthlyMovementLimit());
    acc.setMaintenanceFee(request.getMaintenanceFee());
    acc.setAllowedDayOfMonth(request.getAllowedDayOfMonth());
    acc.setActive(request.getActive());

    if (request.getLinkedCard() != null) {
      LinkedCard lc = new LinkedCard();
      lc.setId(request.getLinkedCard().getId());
      acc.setLinkedCard(lc);
    }

    // Política por cuenta
    acc.setFreeTransactionsLimit(request.getFreeTransactionsLimit());
    acc.setCommissionFee(request.getCommissionFee());

    return acc;
  }

  // Actualiza solo los campos no nulos del request
  public static void mergeIntoEntity(Account target, AccountRequest request) {

    if (target == null || request == null) return;

    // Campos simples
    applyIfPresent(safeTrim(request.getHolderDocument()), target::setHolderDocument);
    applyIfPresent(
        Optional.ofNullable(request.getHolderDocumentType())
            .map(Enum::name)
            .orElse(null),
        target::setHolderDocumentType
    );
    // Enum -> String
    applyIfPresent(ofNullable(request
        .getAccountType())
        .map(Enum::name)
        .orElse(null), target::setAccountType);

    applyIfPresent(request.getBalance(), target::setBalance);
    applyIfPresent(request.getInterestRate(), target::setInterestRate);
    applyIfPresent(request.getMonthlyMovementLimit(), target::setMonthlyMovementLimit);
    applyIfPresent(request.getMaintenanceFee(), target::setMaintenanceFee);
    applyIfPresent(request.getAllowedDayOfMonth(), target::setAllowedDayOfMonth);
    applyIfPresent(request.getActive(), target::setActive);

    // LinkedCard
    if (request.getLinkedCard() != null) {
      if (target.getLinkedCard() == null) target.setLinkedCard(new LinkedCard());
      target.getLinkedCard().setId(request.getLinkedCard().getId());
    }

    // Firmantes
    if (request.getAuthorizedSigners() != null) {
      target.setAuthorizedSigners(cleanDocs(request.getAuthorizedSigners()));
    }

    // Política por cuenta
    applyIfPresent(request.getFreeTransactionsLimit(), target::setFreeTransactionsLimit);
    applyIfPresent(request.getCommissionFee(), target::setCommissionFee);
  }


  private static <T> void applyIfPresent(T value, Consumer<T> setter) {
    ofNullable(value).ifPresent(setter);
  }

  private static String safeTrim(String v) {
    return v == null ? null : v.trim();
  }

  private static List<String> cleanDocs(Collection<String> docs) {
    return ofNullable(docs).orElseGet(Collections::emptyList)
        .stream()
        .filter(Objects::nonNull)
        .map(String::trim)
        .map(s -> s.replaceAll("\\D", "")) // solo dígitos -- [^0-9]
        .filter(s -> !s.isEmpty())
        .filter(s -> DOC_PATTERN.matcher(s).matches())
        .distinct()
        .collect(Collectors.toList());
  }

  private static AccountResponse.AccountTypeEnum toResponseEnum(String type) {
    return ofNullable(type)
        .map(String::trim)
        .flatMap(t -> {
          try {
            return Optional.of(AccountResponse.AccountTypeEnum.valueOf(t));
          } catch (IllegalArgumentException ex) {
            return Optional.empty();
          }
        })
        .orElse(null);
  }

 /* private static AccountResponse.HolderDocumentTypeEnum
  toResponseHolderDocumentTypeEnum(String type) {
    return Optional.ofNullable(type)
        .map(String::trim)
        .flatMap(t -> {
          try {
            return Optional.of(AccountResponse.HolderDocumentTypeEnum.valueOf(t));
          } catch (IllegalArgumentException ex) {
            return Optional.empty();
          }
        })
        .orElse(null);
  }
*/

  private static <T> JsonNullable<T> jn(T value) {
    return value == null ? JsonNullable.undefined() : JsonNullable.of(value);
  }


}
