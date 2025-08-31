package com.nttdata.account_service.integration.transactions.dtos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@Builder
public class TransactionCreateDTO {

    private TransactionProductDTO sender;
    private TransactionProductDTO receiver;
    private String type;
    private BigDecimal amount;
    private TransactionPersonDTO holder;
    private TransactionPersonDTO signatory;

    //Transaction types: deposit, withdrawal, transfer_in, transfer_out, commission
    //Account operational types: deposit, withdrawal, payment, purchase, charge, create, transfer
    public  static final Map<String, String> GET_OPERATIONAL_TYPES = Map.of(
            "deposit", "deposit",
            "withdrawal", "withdrawal",
            "transfer_in","transfer",
            "transfer_out","transfer",
            "commission","charge"
    );

    @JsonCreator
    public TransactionCreateDTO(
            @JsonProperty("sender") TransactionProductDTO sender,
            @JsonProperty("receiver") TransactionProductDTO receiver,
            @JsonProperty("type") String type,
            @JsonProperty("amount") BigDecimal amount,
            @JsonProperty("holder") TransactionPersonDTO holder,
            @JsonProperty("signatory") TransactionPersonDTO signatory
    ){
        this.sender = sender;
        this.receiver = receiver;
        this.type = type;
        this.amount = amount;
        this.holder = holder;
        this.signatory = signatory;

    }
}
