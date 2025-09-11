package com.nttdata.accountservice.integration.customers;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class EligibilityResponseTest {
  @Test
  void gettersSetters() {
    EligibilityResponse r = new EligibilityResponse();
    r.setCustomerId("C1");
    r.setType("PERSONAL");
    r.setProfile("VIP");
    r.setHasActiveCreditCard(true);
    assertEquals("C1", r.getCustomerId());
    assertEquals("PERSONAL", r.getType());
    assertEquals("VIP", r.getProfile());
    assertTrue(r.getHasActiveCreditCard());
    assertNotNull(r.toString());
  }
}
