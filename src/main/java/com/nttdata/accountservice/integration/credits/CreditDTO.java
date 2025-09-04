package com.nttdata.accountservice.integration.credits;

import lombok.*;

@Data
public class CreditDTO {
  private String id;
  private String customerId;
  private String type;   // PERSONAL | BUSINESS | CREDIT_CARD
  private String status; // ACTIVE | INACTIVE
}
