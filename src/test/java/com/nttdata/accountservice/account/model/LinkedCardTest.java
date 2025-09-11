package com.nttdata.accountservice.account.model;


import com.nttdata.accountservice.model.entity.*;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class LinkedCardTest {
  @Test
  void gettersSetters() {
    LinkedCard lc = new LinkedCard();
    lc.setId("PRUEBAID_LC");
    assertEquals("PRUEBAID_LC", lc.getId());
    assertNotNull(lc.toString());
  }
}
