package com.nttdata.accountservice.config;

/**
 *
 */
public class BusinessException extends RuntimeException {
  /**
   * @param message
   */
  public BusinessException(String message) {
    super(message);
  }
}
