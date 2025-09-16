package com.nttdata.accountservice.account.mapper;

import com.nttdata.accountservice.model.*;
import com.nttdata.accountservice.service.impl.*;
import org.junit.jupiter.api.*;

import java.math.*;
import java.util.*;

import static com.nttdata.accountservice.model.AccountRequest.HolderDocumentTypeEnum.*;
import static org.junit.jupiter.api.Assertions.*;

class AccountMapperExtraTest {
  @Test
  void toResponse_mapeaCamposBasicos() {
    com.nttdata.accountservice.model.entity.Account e = new com.nttdata.accountservice.model.entity.Account();
    e.setId("CHECK123");
    e.setAccountType("CHECKING");
    e.setMonthlyMovementLimit(7);

    AccountResponse r = AccountMapper.toResponse(e);

    assertEquals("CHECK123", r.getId());
    assertNotNull(r.getAccountType());
    assertNotNull(r.getMonthlyMovementLimit());
    assertTrue(r.getMonthlyMovementLimit().isPresent());
    assertEquals(7, r.getMonthlyMovementLimit().get().intValue());
  }

  @Test
  void mergeIntoEntity_actualizaSoloCamposPresentes() {
    com.nttdata.accountservice.model.entity.Account target = new com.nttdata.accountservice.model.entity.Account();
    target.setMonthlyMovementLimit(5);

    AccountRequest req = new AccountRequest()
        .monthlyMovementLimit(10);

    AccountMapper.mergeIntoEntity(target, req);

    assertEquals(10, target.getMonthlyMovementLimit());
  }

  //AccountMapperCleanDocsTest
  @Test
  void toEntity_cleanDocs_filtraYNormaliza() {
    AccountRequest rq = new AccountRequest()
        .holderDocumentType(DNI)
        .holderDocument("  12345678 ")
        .authorizedSigners(Arrays.asList(
            null, "  123-45x6 ", "abc", "   ", "12345678", "12345678", "  000012345  "
        ))
        .accountType(AccountRequest.AccountTypeEnum.SAVINGS)
        .balance(new BigDecimal("10"));

    com.nttdata.accountservice.model.entity.Account e = AccountMapper.toEntity(rq);

    // sólo quedan firmantes válidos: 8-11 dígitos, normalizados y sin duplicados
    List<String> signers = e.getAuthorizedSigners();
    assertEquals(List.of("12345678", "000012345"), signers);
    assertEquals("12345678", e.getHolderDocument());
  }

  @Test
  void toResponse_enumsDesconocidos_devuelvenNull() {
    com.nttdata.accountservice.model.entity.Account e = new com.nttdata.accountservice.model.entity.Account();
    e.setHolderDocument("  99999999 ");
    e.setHolderDocumentType("XYZ");
    e.setAccountType("UNKNOWN");

    AccountResponse r = AccountMapper.toResponse(e);
    assertNull(r.getHolderDocumentType());
    assertNull(r.getAccountType());
    assertEquals("99999999", r.getHolderDocument());
  }

  @Test
  void toResponse_linkedCardSeMapeaCorrectamente() {
    com.nttdata.accountservice.model.entity.Account acc = new com.nttdata.accountservice.model.entity.Account();
    LinkedCard lc = new LinkedCard();
    lc.setId("LC1");
    acc.setLinkedCard(lc);

    AccountResponse resp = AccountMapper.toResponse(acc);

    assertNotNull(resp.getLinkedCard());
    assertEquals("LC1", resp.getLinkedCard().getId());
  }

  @Test
  void toEntity_linkedCardSeMapeaCorrectamente() {
    AccountRequest req = new AccountRequest();
    com.nttdata.accountservice.model.LinkedCard lc = new com.nttdata.accountservice.model.LinkedCard();
    lc.setId("LC2");
    req.setLinkedCard(lc);

    com.nttdata.accountservice.model.entity.Account acc = AccountMapper.toEntity(req);

    assertNotNull(acc.getLinkedCard());
    assertEquals("LC2", acc.getLinkedCard().getId());
  }

  @Test
  void mergeIntoEntity_linkedCardSeActualiza() {
    // Caso 1: target no tiene linkedCard
    AccountRequest req = new AccountRequest();
    com.nttdata.accountservice.model.LinkedCard lc = new com.nttdata.accountservice.model.LinkedCard();
    lc.setId("LC3");
    req.setLinkedCard(lc);

    com.nttdata.accountservice.model.entity.Account target = new com.nttdata.accountservice.model.entity.Account(); // no tiene linkedCard

    AccountMapper.mergeIntoEntity(target, req);

    assertNotNull(target.getLinkedCard());
    assertEquals("LC3", target.getLinkedCard().getId());

    // Caso 2: target ya tiene linkedCard
    com.nttdata.accountservice.model.entity.Account target2 = new com.nttdata.accountservice.model.entity.Account();
    LinkedCard existingLc = new LinkedCard();
    existingLc.setId("OLD");
    target2.setLinkedCard(existingLc);

    lc.setId("LC4");
    req.setLinkedCard(lc);

    AccountMapper.mergeIntoEntity(target2, req);

    assertEquals("LC4", target2.getLinkedCard().getId());
  }

  @Test
  void mergeIntoEntity_firmantesSeNormalizan() {
    AccountRequest req = new AccountRequest()
        .authorizedSigners(List.of("  12345678 ", "ABC", "00001111222"));

    com.nttdata.accountservice.model.entity.Account target = new com.nttdata.accountservice.model.entity.Account();

    AccountMapper.mergeIntoEntity(target, req);

    assertEquals(List.of("12345678", "00001111222"), target.getAuthorizedSigners());
  }

  @Test
  void toEntity_conRequestNull() {
    assertNull(AccountMapper.toEntity(null));
  }

  @Test
  void mergeIntoEntity_conTargetNull() {
    AccountMapper.mergeIntoEntity(null, new AccountRequest());
  }

  @Test
  void mergeIntoEntity_conRequestNull() {
    AccountMapper.mergeIntoEntity(new com.nttdata.accountservice.model.entity.Account(), null);
  }


}
