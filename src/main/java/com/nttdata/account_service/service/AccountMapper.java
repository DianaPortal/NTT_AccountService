package com.nttdata.account_service.service;

import com.nttdata.account_service.model.AccountRequest;
import com.nttdata.account_service.model.AccountResponse;
import com.nttdata.account_service.model.entity.Account;
import com.nttdata.account_service.model.entity.LinkedCard;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;


public class AccountMapper {


    public static AccountResponse toResponse(Account account) {
        if (account == null) return null;

        AccountResponse response = new AccountResponse();
        response.setId(account.getId());
        response.setAccountNumber(account.getAccountNumber());
        response.setInterbankNumber(account.getInterbankNumber());
        response.setHolderDocument(account.getHolderDocument());

        List<String> cleanSigners = Optional.ofNullable(account.getAuthorizedSigners())
                .orElseGet(Collections::emptyList)
                .stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());
        response.setAuthorizedSigners(cleanSigners);

        // accountType se guarda como String en la entity, se convierte al enum del contrato
        if (account.getAccountType() != null) {
            try {
                response.setAccountType(AccountResponse.AccountTypeEnum.fromValue(account.getAccountType()));
            } catch (IllegalArgumentException ex) {
                response.setAccountType(null); // valor inesperado en BD
            }
        }

        response.setBalance(Optional.ofNullable(account.getBalance()).orElse(BigDecimal.ZERO));
        response.setInterestRate(account.getInterestRate());
        response.setMonthlyMovementLimit(account.getMonthlyMovementLimit());
        response.setMaintenanceFee(account.getMaintenanceFee());
        response.setAllowedDayOfMonth(account.getAllowedDayOfMonth());
        response.setCreationDate(account.getCreationDate());
        response.setActive(account.getActive());

        if (account.getLinkedCard() != null) {
            var linkedCard = new com.nttdata.account_service.model.LinkedCard();
            linkedCard.setId(account.getLinkedCard().getId());
            response.setLinkedCard(linkedCard);
        }
        return response;
    }

    public static Account toEntity(AccountRequest request) {
        if (request == null) return null;

        Account account = new Account();
        account.setId(request.getId());
        account.setAccountNumber(request.getAccountNumber());
        account.setInterbankNumber(request.getInterbankNumber());
        account.setHolderDocument(request.getHolderDocument());

        List<String> cleanSigners = Optional.ofNullable(request.getAuthorizedSigners())
                .orElseGet(Collections::emptyList)
                .stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());
        account.setAuthorizedSigners(cleanSigners);

        // Guardamos el tipo como String
        account.setAccountType(request.getAccountType() != null ? request.getAccountType().toString() : null);

        account.setBalance(request.getBalance());
        account.setInterestRate(request.getInterestRate());
        account.setMonthlyMovementLimit(request.getMonthlyMovementLimit());
        account.setMaintenanceFee(request.getMaintenanceFee());
        account.setAllowedDayOfMonth(request.getAllowedDayOfMonth());
        account.setCreationDate(request.getCreationDate());
        account.setActive(request.getActive());

        if (request.getLinkedCard() != null) {
            LinkedCard linkedCard = new LinkedCard();
            linkedCard.setId(request.getLinkedCard().getId());
            account.setLinkedCard(linkedCard);
        }
        return account;
    }

    public static void mergeIntoEntity(Account target, AccountRequest request) {
        if (target == null || request == null) return;

        if (request.getAccountNumber() != null) target.setAccountNumber(request.getAccountNumber());
        if (request.getInterbankNumber() != null) target.setInterbankNumber(request.getInterbankNumber());
        if (request.getHolderDocument() != null) target.setHolderDocument(request.getHolderDocument());

        if (request.getAuthorizedSigners() != null) {
            List<String> cleanSigners = request.getAuthorizedSigners()
                    .stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .distinct()
                    .collect(Collectors.toList());
            target.setAuthorizedSigners(cleanSigners);
        }

        if (request.getAccountType() != null) target.setAccountType(request.getAccountType().toString());
        if (request.getBalance() != null) target.setBalance(request.getBalance());
        if (request.getInterestRate() != null) target.setInterestRate(request.getInterestRate());
        if (request.getMonthlyMovementLimit() != null) target.setMonthlyMovementLimit(request.getMonthlyMovementLimit());
        if (request.getMaintenanceFee() != null) target.setMaintenanceFee(request.getMaintenanceFee());
        if (request.getAllowedDayOfMonth() != null) target.setAllowedDayOfMonth(request.getAllowedDayOfMonth());
        if (request.getCreationDate() != null) target.setCreationDate(request.getCreationDate());
        if (request.getActive() != null) target.setActive(request.getActive());

        if (request.getLinkedCard() != null) {
            if (target.getLinkedCard() == null) target.setLinkedCard(new LinkedCard());
            target.getLinkedCard().setId(request.getLinkedCard().getId());
        }
    }
}
