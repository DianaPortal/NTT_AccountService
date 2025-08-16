package com.nttdata.account_service.service;

import com.nttdata.account_service.model.AccountRequest;
import com.nttdata.account_service.model.AccountResponse;
import com.nttdata.account_service.api.ApiApiDelegate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountApiDelegateImpl implements ApiApiDelegate {

    private final AccountService accountService;

    @Override
    public ResponseEntity<List<AccountResponse>> listAccounts() {
        return accountService.listAccounts();
    }

    @Override
    public ResponseEntity<AccountResponse> getAccountById(String id) {
        return accountService.getAccountById(id);
    }

    @Override
    public ResponseEntity<AccountResponse> registerAccount(AccountRequest accountRequest) {
        return accountService.createAccount(accountRequest);
    }

    @Override
    public ResponseEntity<AccountResponse> updateAccount(String id, AccountRequest accountRequest) {
        return accountService.updateAccount(id, accountRequest);
    }

    @Override
    public ResponseEntity<Void> deleteAccount(String id) {
        return accountService.deleteAccount(id);
    }
}