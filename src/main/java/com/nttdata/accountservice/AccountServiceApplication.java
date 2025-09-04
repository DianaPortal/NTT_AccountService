package com.nttdata.accountservice;

import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.data.mongodb.repository.config.*;

/**
 * Clase principal del microservicio de cuentas.
 * Inicia el contexto de Spring Boot y habilita los repositorios reactivos.
 */

@SpringBootApplication
@EnableReactiveMongoRepositories(basePackages = "com.nttdata.accountservice.repository")
public class AccountServiceApplication {
  /**
   * Método principal que inicia la aplicación Spring Boot.
   */

  public static void main(String[] args) {
    SpringApplication.run(AccountServiceApplication.class, args);
  }

}
