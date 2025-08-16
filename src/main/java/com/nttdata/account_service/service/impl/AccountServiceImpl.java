package com.nttdata.account_service.service.impl;

import com.nttdata.account_service.model.entity.Account;
import com.nttdata.model.LinkedCard;
import com.nttdata.account_service.repository.AccountRepository;
import com.nttdata.account_service.service.AccountService;
import com.nttdata.model.AccountResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;

    @Override
    public ResponseEntity<List<AccountResponse>> listAccounts() {
        List<AccountResponse> responses = accountRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    private AccountResponse toResponse(Account account) {
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
            LinkedCard card = new LinkedCard();
            card.setId(account.getLinkedCard().getId());
            response.setLinkedCard(card);
        }

        return response;
    }

}
