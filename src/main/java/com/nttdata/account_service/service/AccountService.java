package com.nttdata.account_service.service;

import com.nttdata.account_service.model.AccountRequest;
import com.nttdata.account_service.model.AccountResponse;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface AccountService {
    ResponseEntity<List<AccountResponse>> listAccounts();

    ResponseEntity<AccountResponse> getAccountById(String id);

    ResponseEntity<AccountResponse> createAccount(AccountRequest request);

    ResponseEntity<AccountResponse> updateAccount(String id, AccountRequest request);

    ResponseEntity<Void> deleteAccount(String id);
}
