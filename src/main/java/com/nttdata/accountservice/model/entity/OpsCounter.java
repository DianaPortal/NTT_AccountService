package com.nttdata.accountservice.model.entity;

import lombok.*;

@Data
public class OpsCounter {
  private String yearMonth; // "YYYY-MM"
  private Integer count;    // operaciones del mes que CUENTAN

}
