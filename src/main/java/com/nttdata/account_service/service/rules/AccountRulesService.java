package com.nttdata.account_service.service.rules;

import com.nttdata.account_service.config.BusinessException;
import com.nttdata.account_service.model.AccountRequest;
import com.nttdata.account_service.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AccountRulesService {

    private final AccountRepository accountRepository;

    public Mono<Void> validateLegacyRules(String holderDocument,
                                          AccountRequest.AccountTypeEnum reqType,
                                          String customerType) {
        return accountRepository.findByHolderDocument(holderDocument)
                .collectList()
                .flatMap(existing -> {
                    if ("BUSINESS".equals(customerType)) {
                        if (reqType == AccountRequest.AccountTypeEnum.SAVINGS
                                || reqType == AccountRequest.AccountTypeEnum.FIXED_TERM) {
                            return Mono.error(new BusinessException("Cliente BUSINESS no puede abrir SAVINGS ni FIXED_TERM."));
                        }
                        return Mono.empty();
                    }
                    // PERSONAL: máx 1 SAVINGS y máx 1 CHECKING
                    boolean hasSavings = existing.stream().anyMatch(a -> "SAVINGS".equalsIgnoreCase(a.getAccountType()));
                    boolean hasChecking = existing.stream().anyMatch(a -> "CHECKING".equalsIgnoreCase(a.getAccountType()));
                    if (reqType == AccountRequest.AccountTypeEnum.SAVINGS && hasSavings) {
                        return Mono.error(new BusinessException("Cliente PERSONAL ya tiene una cuenta de tipo SAVINGS."));
                    }
                    if (reqType == AccountRequest.AccountTypeEnum.CHECKING && hasChecking) {
                        return Mono.error(new BusinessException("Cliente PERSONAL ya tiene una cuenta de tipo CHECKING."));
                    }
                    return Mono.empty();
                });
    }
}