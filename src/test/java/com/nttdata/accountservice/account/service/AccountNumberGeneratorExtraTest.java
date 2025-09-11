package com.nttdata.accountservice.account.service;

import com.nttdata.accountservice.util.*;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class AccountNumberGeneratorExtraTest {
  @Test
  void numeric_longitudesYFormatoOk() {
    String n11 = AccountNumberGenerator.numeric(11);
    String n20 = AccountNumberGenerator.numeric(20);

    assertEquals(11, n11.length());
    assertEquals(20, n20.length());
    assertTrue(n11.matches("\\d{11}"));
    assertTrue(n20.matches("\\d{20}"));
  }

  @Test
  void numeric_consecutivosDifieren() {
    String a = AccountNumberGenerator.numeric(11);
    String b = AccountNumberGenerator.numeric(11);
    assertNotEquals(a, b);
  }
}
