package com.nttdata.account_service.service;

import com.nttdata.account_service.model.entity.Account;
import com.nttdata.account_service.model.entity.LinkedCard;
import com.nttdata.account_service.model.AccountRequest;
import com.nttdata.account_service.model.AccountResponse;


public class AccountMapper {

    public static AccountResponse toResponse(Account account) {
        AccountResponse response = new AccountResponse();
        response.setId(account.getId());
        response.setAccountNumber(account.getAccountNumber());
        response.setInterbankNumber(account.getInterbankNumber());
        response.setHolderDocument(account.getHolderDocument());
        response.setAuthorizedSigners(account.getAuthorizedSigners());

        if (account.getAccountType() != null) {
            response.setAccountType(AccountResponse.AccountTypeEnum.fromValue(account.getAccountType()));
        }

        response.setBalance(account.getBalance());
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
        Account account = new Account();
        account.setId(request.getId());
        account.setAccountNumber(request.getAccountNumber());
        account.setInterbankNumber(request.getInterbankNumber());
        account.setHolderDocument(request.getHolderDocument());
        account.setAuthorizedSigners(request.getAuthorizedSigners());
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
}