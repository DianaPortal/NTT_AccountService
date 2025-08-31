package com.nttdata.account_service.integration.transactions.dtos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
public class TransactionProductDTO {
    private String id;
    private String type;
    private String number;
    private BigDecimal balance;
    private BigDecimal limit;

    //SAVINGS, CHECKING, FIXED_TERM
    //savings_account, checking_account, fixed_term_account
    public  static final Map<String, String> GET_PRODUCT_TYPES = Map.of(
            "SAVINGS", "savings_account",
            "CHECKING", "checking_account",
            "FIXED_TERM","fixed_term_account"
    );

    @JsonCreator
    @Builder
    public TransactionProductDTO(
            @JsonProperty("id") String id,
            @JsonProperty("type") String type,
            @JsonProperty("number") String number,
            @JsonProperty("balance") BigDecimal balance,
            @JsonProperty("limit") BigDecimal limit
    ){
        this.id = id;
        this.type = type;
        this.number = number;
        this.balance = balance;
        this.limit = limit;
    }



}
