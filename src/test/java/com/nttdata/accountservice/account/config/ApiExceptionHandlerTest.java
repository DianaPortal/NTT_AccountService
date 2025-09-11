package com.nttdata.accountservice.account.config;

import com.nttdata.accountservice.config.*;
import com.nttdata.accountservice.model.*;
import org.junit.jupiter.api.*;
import org.springframework.dao.*;
import org.springframework.web.server.*;
import reactor.test.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.HttpStatus.*;


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

  @Test
  void badRequest_400() {
    var h = new ApiExceptionHandler();
    StepVerifier.create(h.handleBadRequest(new IllegalArgumentException("bad")))
        .assertNext(re -> {
          assertEquals(400, re.getStatusCodeValue());
          assertEquals("bad", Objects.requireNonNull(re.getBody()).getMessage());
        })
        .verifyComplete();
  }

  @Test
  void business_422() {
    var h = new ApiExceptionHandler();
    StepVerifier.create(h.handleBusiness(new BusinessException("negocio")))
        .assertNext(re -> {
          assertEquals(UNPROCESSABLE_ENTITY.value(), re.getStatusCodeValue());
          assertEquals("negocio", Objects.requireNonNull(re.getBody()).getMessage());
        })
        .verifyComplete();
  }

  @Test
  void internal_500() {
    var h = new ApiExceptionHandler();
    StepVerifier.create(h.handleInternal(new RuntimeException("boom")))
        .assertNext(re -> {
          assertEquals(INTERNAL_SERVER_ERROR.value(), re.getStatusCodeValue());
          assertEquals("Error interno", Objects.requireNonNull(re.getBody()).getMessage());
        })
        .verifyComplete();
  }

  @Test
  void status_reasonNull_usaMensajePorDefecto() {
    var h = new ApiExceptionHandler();
    StepVerifier.create(h.handleStatus(new ResponseStatusException(I_AM_A_TEAPOT)))
        .assertNext(re -> {
          assertEquals(I_AM_A_TEAPOT.value(), re.getStatusCodeValue());
          assertEquals("Error", Objects.requireNonNull(re.getBody()).getMessage());
        })
        .verifyComplete();
  }
}
