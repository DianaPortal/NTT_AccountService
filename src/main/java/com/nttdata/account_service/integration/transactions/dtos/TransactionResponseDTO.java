package com.nttdata.account_service.integration.transactions.dtos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
public class TransactionResponseDTO {
    private String id;
    private int number;
    private TransactionProductDTO product;
    private TransactionProductDTO receiver;
    private String type;
    private TransactionPersonDTO client;
    private TransactionPersonDTO signatory;
    private double amount;
    //@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "\"yyyy-MM-dd'T'HH:mm:ssXXX\"")
    private OffsetDateTime createdDate;

    @JsonCreator
    public TransactionResponseDTO(
            @JsonProperty("id") String id,
            @JsonProperty("number") int number,
            @JsonProperty("sender") TransactionProductDTO product,
            @JsonProperty("receiver") TransactionProductDTO receiver,
            @JsonProperty("type") String type,
            @JsonProperty("amount") double amount,
            @JsonProperty("createdDate") OffsetDateTime createdDate,
            @JsonProperty("holder") TransactionPersonDTO client,
            @JsonProperty("signatory") TransactionPersonDTO signatory
    ){
        this.id = id;
        this.number = number;
        this.product = product;
        this.receiver = receiver;
        this.type = type;
        this.amount = amount;
        this.createdDate = createdDate;
        this.client = client;
        this.signatory = signatory;

    }

}