package com.nttdata.accountservice.integration.credits;


import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class CreditDTOTest {
  @Test
  void gettersSetters() {
    CreditDTO d = new CreditDTO();
    d.setId("1");
    d.setCustomerId("C1");
    d.setType("CREDIT_CARD");
    d.setStatus("ACTIVE");
    assertEquals("1", d.getId());
    assertEquals("C1", d.getCustomerId());
    assertEquals("CREDIT_CARD", d.getType());
    assertEquals("ACTIVE", d.getStatus());
    assertNotNull(d.toString());
  }
}
