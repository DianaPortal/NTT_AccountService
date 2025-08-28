package com.nttdata.account_service.service.policy;

import com.nttdata.account_service.model.AccountRequest;
import com.nttdata.account_service.model.entity.Account;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AccountPolicyService {

    @Value("${policy.savings.freeOps:5}")
    private Integer savingsFreeOps;
    @Value("${policy.savings.fee:1.50}")
    private BigDecimal savingsFee;

    @Value("${policy.checking.freeOps:10}")
    private Integer checkingFreeOps;
    @Value("${policy.checking.fee:0.90}")
    private BigDecimal checkingFee;

    @Value("${policy.fixed.freeOps:0}")
    private Integer fixedFreeOps;
    @Value("${policy.fixed.fee:0.00}")
    private BigDecimal fixedFee;

    public void applyDefaults(Account acc, AccountRequest.AccountTypeEnum type) {
        if (acc.getFreeTransactionsLimit() == null || acc.getFreeTransactionsLimit() < 0) {
            switch (type) {
                case SAVINGS:
                    acc.setFreeTransactionsLimit(savingsFreeOps);
                    break;
                case CHECKING:
                    acc.setFreeTransactionsLimit(checkingFreeOps);
                    break;
                case FIXED_TERM:
                    acc.setFreeTransactionsLimit(fixedFreeOps);
                    break;
            }
        }
        if (acc.getCommissionFee() == null || acc.getCommissionFee().compareTo(BigDecimal.ZERO) < 0) {
            switch (type) {
                case SAVINGS:
                    acc.setCommissionFee(savingsFee);
                    break;
                case CHECKING:
                    acc.setCommissionFee(checkingFee);
                    break;
                case FIXED_TERM:
                    acc.setCommissionFee(fixedFee);
                    break;
            }
        }
    }
}