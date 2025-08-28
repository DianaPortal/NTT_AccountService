package com.nttdata.account_service.config;

public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
