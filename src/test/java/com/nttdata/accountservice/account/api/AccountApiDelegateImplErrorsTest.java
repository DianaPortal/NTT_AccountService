package com.nttdata.accountservice.account.api;


import com.nttdata.accountservice.api.*;
import com.nttdata.accountservice.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.*;
import org.mockito.junit.jupiter.*;
import org.springframework.http.*;
import org.springframework.web.server.*;
import reactor.core.publisher.*;
import reactor.test.*;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AccountApiDelegateImplErrorsTest {

  @Mock
  AccountService service;
  AccountApiDelegateImpl delegate;

  @BeforeEach
  void setUp() {
    delegate = new AccountApiDelegateImpl(service);
  }

  @Test
  void getById_notFound_propagates404() {
    when(service.getAccountById("ZZZ"))
        .thenReturn(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)));

    StepVerifier.create(delegate.getAccountById("ZZZ", null))
        .expectError(ResponseStatusException.class)
        .verify();
  }

}
