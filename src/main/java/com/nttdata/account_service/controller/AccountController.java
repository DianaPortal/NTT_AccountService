package com.nttdata.account_service.controller;
import com.nttdata.account_service.service.AccountService;
import com.nttdata.model.AccountResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequiredArgsConstructor
public class AccountController implements com.nttdata.api.ApiApi {

    private final AccountService accountService;

    @Override
    public ResponseEntity<List<AccountResponse>> listAccounts() {
        return accountService.listAccounts();
    }
}
