package com.nttdata.account_service.integration.customers;

import lombok.Data;

@Data
public class EligibilityResponse {
    private String customerId;          // Id del cliente en Customers
    private String type;                // PERSONAL | BUSINESS
    private String profile;             // STANDARD | VIP | PYME
    private Boolean hasActiveCreditCard;
}
