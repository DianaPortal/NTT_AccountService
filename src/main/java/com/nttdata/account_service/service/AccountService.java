package com.nttdata.account_service.service;

import com.nttdata.account_service.model.entity.Account;
import com.nttdata.model.AccountResponse;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface AccountService {
    ResponseEntity<List<AccountResponse>> listAccounts();
}
