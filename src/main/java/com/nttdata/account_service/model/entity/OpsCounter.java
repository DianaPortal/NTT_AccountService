package com.nttdata.account_service.model.entity;

import lombok.Data;

@Data
public class OpsCounter {
    private String yearMonth; // "YYYY-MM"
    private Integer count;    // operaciones del mes que CUENTAN

}
