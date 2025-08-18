package com.nttdata.account_service.config;

import com.nttdata.account_service.model.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleNotFound(IllegalArgumentException ex) {
        log.warn("Error 404: {}", ex.getMessage());
        return Mono.just(buildErrorResponse(HttpStatus.NOT_FOUND, ex));
    }

    @ExceptionHandler(IllegalStateException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleBadRequest(IllegalStateException ex) {
        log.warn("Error 400: {}", ex.getMessage());
        return Mono.just(buildErrorResponse(HttpStatus.BAD_REQUEST, ex));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleInternalError(Exception ex) {
        log.error("Error 500: {}", ex.getMessage(), ex);
        return Mono.just(buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex));
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(HttpStatus status, Exception ex) {
        ErrorResponse error = new ErrorResponse()
                .timestamp(OffsetDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(ex.getMessage());
        return ResponseEntity.status(status).body(error);
    }

}
