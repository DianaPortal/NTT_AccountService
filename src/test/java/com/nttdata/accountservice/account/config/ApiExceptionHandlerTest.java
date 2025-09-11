package com.nttdata.accountservice.account.config;

import com.nttdata.accountservice.config.*;
import com.nttdata.accountservice.model.*;
import org.junit.jupiter.api.*;
import org.springframework.dao.*;
import org.springframework.web.server.*;
import reactor.test.*;

import static org.junit.jupiter.api.Assertions.*;

class ApiExceptionHandlerTest {
  @Test
  void duplicateKey_devuelve409() {
    ApiExceptionHandler h = new ApiExceptionHandler();
    StepVerifier.create(h.handleConflict(new DuplicateKeyException("dup")))
        .assertNext(re -> {
          assertEquals(409, re.getStatusCodeValue());
          ErrorResponse body = re.getBody();
          assertNotNull(body);
          assertEquals(409, body.getStatus());
        })
        .verifyComplete();
  }

  @Test
  void responseStatus_preservaCodigo() {
    ApiExceptionHandler h = new ApiExceptionHandler();
    StepVerifier.create(h.handleStatus(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "x")))
        .assertNext(re -> assertEquals(404, re.getStatusCodeValue()))
        .verifyComplete();
  }
}
