package com.nttdata.account_service.integration.credits;

import lombok.Data;

@Data
public class CreditDTO {
    private String id;
    private String customerId;
    private String type;   // PERSONAL | BUSINESS | CREDIT_CARD
    private String status; // ACTIVE | INACTIVE
}
