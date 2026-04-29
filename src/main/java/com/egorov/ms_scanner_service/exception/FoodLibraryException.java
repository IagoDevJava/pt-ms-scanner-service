package com.egorov.ms_scanner_service.exception;

/**
 * Исключение при недоступности сервиса библиотеки продуктов.
 */
public class FoodLibraryException extends ScanException {

  public FoodLibraryException(String message) {
    super(message);
  }

  public FoodLibraryException(String message, Throwable cause) {
    super(message, cause);
  }
}