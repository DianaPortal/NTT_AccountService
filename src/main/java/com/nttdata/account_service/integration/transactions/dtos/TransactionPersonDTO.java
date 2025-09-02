package com.nttdata.account_service.integration.transactions.dtos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@Builder
public class TransactionPersonDTO {
    private String id;
    private String document;
    private String fullName;
    private String type;

    //PERSONAL, BUSINESS
    //personal, business
    public  static final Map<String, String> GET_PERSON_TYPES = Map.of(
            "PERSONAL", "personal",
            "BUSINESS", "business"
    );

    @JsonCreator
    public TransactionPersonDTO(
            @JsonProperty("id") String id,
            @JsonProperty("document") String document,
            @JsonProperty("type") String type,
            @JsonProperty("fullName") String fullName
    ){
        this.id = id;
        this.document = document;
        this.type = type;
        this.fullName = fullName;
    }


}