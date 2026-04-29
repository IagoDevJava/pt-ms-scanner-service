package com.egorov.ms_scanner_service.exception;

/**
 * Базовое исключение для всех ошибок сканирования.
 */
public class ScanException extends RuntimeException {

  public ScanException(String message) {
    super(message);
  }

  public ScanException(String message, Throwable cause) {
    super(message, cause);
  }
}