package com.nttdata.accountservice.account.model;

import com.nttdata.accountservice.model.entity.*;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class OpsCounterTest {
  @Test
  void gettersSetters() {
    OpsCounter oc = new OpsCounter();
    oc.setYearMonth("2025-09");
    oc.setCount(3);
    assertEquals("2025-09", oc.getYearMonth());
    assertEquals(3, oc.getCount());
    assertNotNull(oc.toString());
  }
}