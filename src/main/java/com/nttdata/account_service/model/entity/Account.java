package com.nttdata.account_service.model.entity;


import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


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
    private LinkedCard linkedCard;
    // Nuevos campos entregable II
    private Integer freeTransactionsLimit;// operaciones sin comisión
    private BigDecimal commissionFee;
    /// / comisión por transacción extra

    private ArrayList<String> opIds;
    private OpsCounter opsCounter;

}


