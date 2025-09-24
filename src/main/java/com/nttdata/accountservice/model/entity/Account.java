package com.nttdata.accountservice.model.entity;


import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.*;
import org.springframework.data.mongodb.core.mapping.*;

import java.math.*;
import java.time.*;
import java.util.*;


/**
 *
 */
@Data
@Document(collection = "accounts")
public class Account {
  @Id
  private String id;
  @Indexed(unique = true)
  private String accountNumber;  // 11 dígitos
  @Indexed(unique = true)
  private String interbankNumber;  // 20 dígitos
  // Titulares / firmantes
  private String holderDocument;
  private String holderDocumentType;
  private List<String> authorizedSigners;
  // Tipo y estado
  private String accountType;  // SAVINGS, CHECKING, FIXED_TERM
  private Boolean active;
  // Saldos y tasas
  private BigDecimal balance;
  private BigDecimal interestRate;
  //Reglas
  private Integer monthlyMovementLimit; // solo SAVINGS
  private BigDecimal maintenanceFee;    // solo CHECKING
  private Integer allowedDayOfMonth;    // solo FIXED_TERM

  private LocalDate creationDate;
  // Tarjeta vinculada
  private com.nttdata.accountservice.model.LinkedCard linkedCard;
  // Nuevos campos entregable II
  private Integer freeTransactionsLimit;// operaciones sin comisión
  private BigDecimal commissionFee;
  /// / comisión por transacción extra

  private ArrayList<String> opIds;
  private com.nttdata.accountservice.model.entity.OpsCounter opsCounter;

  private LocalDate openingDate; // solo FIXED_TERM
  private LocalDate maturityDate; // solo FIXED_TERM
  private BigDecimal earlyWithdrawalPenalty; // % penalización por retiro anticipado
  private Integer term; // plazo en meses
}


