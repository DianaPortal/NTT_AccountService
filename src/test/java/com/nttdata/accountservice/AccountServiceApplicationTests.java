package com.nttdata.accountservice;

import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.*;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
/*@TestPropertySource(properties = {
		"spring.cloud.config.enabled=false",
		"spring.cloud.config.import-check.enabled=false",
		"server.port=0"
})*/
class AccountServiceApplicationTests {

  @Test
  void contextLoads() {
    // Prueba vacía, solo verifica que el contexto de Spring se carga sin errores
    assertTrue(true);
  }


}
