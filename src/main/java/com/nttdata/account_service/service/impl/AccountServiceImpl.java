package com.nttdata.account_service.service.impl;

import com.nttdata.account_service.model.AccountRequest;
import com.nttdata.account_service.model.AccountResponse;
import com.nttdata.account_service.model.entity.Account;
import com.nttdata.account_service.service.AccountMapper;
import com.nttdata.account_service.repository.AccountRepository;

import com.nttdata.account_service.service.AccountService;
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
                .map(AccountMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @Override
    public ResponseEntity<AccountResponse> getAccountById(String id) {
        return accountRepository.findById(id)
                .map(AccountMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }



    @Override
    public ResponseEntity<AccountResponse> createAccount(AccountRequest request) {
        Account account = AccountMapper.toEntity(request);
        Account saved = accountRepository.save(account);
        return ResponseEntity.ok(AccountMapper.toResponse(saved));
    }

    @Override
    public ResponseEntity<AccountResponse> updateAccount(String id, AccountRequest request) {
        return accountRepository.findById(id)
                .map(existing -> {
                    Account account = AccountMapper.toEntity(request);
                    account.setId(existing.getId());
                    Account updated = accountRepository.save(account);
                    return ResponseEntity.ok(AccountMapper.toResponse(updated));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<Void> deleteAccount(String id) {
        return accountRepository.findById(id)
                .map(account -> {
                    accountRepository.deleteById(id);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
