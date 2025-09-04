package com.nttdata.accountservice.account.mapper;

import com.nttdata.accountservice.model.*;
import com.nttdata.accountservice.model.entity.*;
import com.nttdata.accountservice.service.impl.*;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

public class AccountMapperExtraTest {
  @Test
  void toResponse_mapeaCamposBasicos() {
    Account e = new Account();
    e.setId("A1");
    e.setAccountType("CHECKING");
    e.setMonthlyMovementLimit(7);

    AccountResponse r = AccountMapper.toResponse(e);

    assertEquals("A1", r.getId());
    assertNotNull(r.getAccountType());
    assertNotNull(r.getMonthlyMovementLimit());
    assertTrue(r.getMonthlyMovementLimit().isPresent());
    assertEquals(7, r.getMonthlyMovementLimit().get().intValue());
  }

  @Test
  void mergeIntoEntity_actualizaSoloCamposPresentes() {
    Account target = new Account();
    target.setMonthlyMovementLimit(5);

    AccountRequest req = new AccountRequest()
        .monthlyMovementLimit(10);

    AccountMapper.mergeIntoEntity(target, req);

    assertEquals(10, target.getMonthlyMovementLimit());
  }
}
