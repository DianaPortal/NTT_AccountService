package com.nttdata.accountservice.config;

import com.nttdata.accountservice.model.*;
import org.slf4j.*;
import org.springframework.dao.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.*;
import reactor.core.publisher.*;

import java.time.*;

@RestControllerAdvice
public class ApiExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

  @ExceptionHandler(IllegalArgumentException.class)
  public Mono<ResponseEntity<ErrorResponse>> handleBadRequest(IllegalArgumentException ex) {
    log.warn("400: {}", ex.getMessage());
    return build(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  @ExceptionHandler(ResponseStatusException.class)
  public Mono<ResponseEntity<ErrorResponse>> handleStatus(ResponseStatusException ex) {
    log.warn("{}: {}", ex.getStatus().value(), ex.getReason());
    return build(HttpStatus.valueOf(ex.getStatus().value()),
        ex.getReason() != null ? ex.getReason() : "Error");
  }

  @ExceptionHandler(DuplicateKeyException.class)
  public Mono<ResponseEntity<ErrorResponse>> handleConflict(DuplicateKeyException ex) {
    log.warn("409: {}", ex.getMessage());
    return build(HttpStatus.CONFLICT, "Conflicto de unicidad");
  }

  @ExceptionHandler(BusinessException.class)
  public Mono<ResponseEntity<ErrorResponse>> handleBusiness(BusinessException ex) {
    log.warn("422: {}", ex.getMessage());
    return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
  }

  @ExceptionHandler(Exception.class)
  public Mono<ResponseEntity<ErrorResponse>> handleInternal(Exception ex) {
    log.error("500: {}", ex.getMessage(), ex);
    return build(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno");
  }

  private Mono<ResponseEntity<ErrorResponse>> build(HttpStatus status, String msg) {
    return Mono.fromSupplier(() ->
        ResponseEntity.status(status).body(
            new ErrorResponse()
                .timestamp(OffsetDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(msg)
        )
    );
  }
}