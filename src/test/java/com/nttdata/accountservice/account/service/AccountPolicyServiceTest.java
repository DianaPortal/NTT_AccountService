package com.nttdata.accountservice.account.service;

import com.nttdata.accountservice.model.*;
import com.nttdata.accountservice.model.entity.*;
import com.nttdata.accountservice.service.policy.*;
import org.junit.jupiter.api.*;
import org.springframework.test.util.*;

import java.math.*;

import static org.junit.jupiter.api.Assertions.*;

public class AccountPolicyServiceTest {
  AccountPolicyService service;

  @BeforeEach
  void setUp() {
    service = new AccountPolicyService();
    // Inyectamos @Value por reflexión
    ReflectionTestUtils.setField(service, "savingsFreeOps", 5);
    ReflectionTestUtils.setField(service, "savingsFee", new BigDecimal("1.50"));
    ReflectionTestUtils.setField(service, "checkingFreeOps", 10);
    ReflectionTestUtils.setField(service, "checkingFee", new BigDecimal("0.90"));
    ReflectionTestUtils.setField(service, "fixedFreeOps", 0);
    ReflectionTestUtils.setField(service, "fixedFee", new BigDecimal("0.00"));
  }

  @Test
  void applyDefaults_savings() {
    Account acc = new Account();
    service.applyDefaults(acc, AccountRequest.AccountTypeEnum.SAVINGS);
    assertEquals(5, acc.getFreeTransactionsLimit());
    assertEquals(0, new BigDecimal("1.50").compareTo(acc.getCommissionFee()));
  }

  @Test
  void applyDefaults_checking() {
    Account acc = new Account();
    service.applyDefaults(acc, AccountRequest.AccountTypeEnum.CHECKING);
    assertEquals(10, acc.getFreeTransactionsLimit());
    assertEquals(0, new BigDecimal("0.90").compareTo(acc.getCommissionFee()));
  }

  @Test
  void applyDefaults_fixedTerm() {
    Account acc = new Account();
    service.applyDefaults(acc, AccountRequest.AccountTypeEnum.FIXED_TERM);
    assertEquals(0, acc.getFreeTransactionsLimit());
    assertEquals(0, new BigDecimal("0.00").compareTo(acc.getCommissionFee()));
  }

  @Test
  void applyDefaults_noOverrideWhenAlreadySet() {
    Account acc = new Account();
    acc.setFreeTransactionsLimit(99);
    acc.setCommissionFee(new BigDecimal("7.77"));
    service.applyDefaults(acc, AccountRequest.AccountTypeEnum.SAVINGS);
    assertEquals(99, acc.getFreeTransactionsLimit());
    assertEquals(0, new BigDecimal("7.77").compareTo(acc.getCommissionFee()));
  }
}
