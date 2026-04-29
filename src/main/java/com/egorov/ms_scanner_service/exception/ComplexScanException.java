package com.egorov.ms_scanner_service.exception;

import java.util.UUID;
import lombok.Getter;

/**
 * Исключение при комплексном сканировании (ошибки очереди, обработки).
 */
@Getter
public class ComplexScanException extends ScanException {

  private final UUID taskId;

  public ComplexScanException(UUID taskId, String message) {
    super(message);
    this.taskId = taskId;
  }

  public ComplexScanException(UUID taskId, String message, Throwable cause) {
    super(message, cause);
    this.taskId = taskId;
  }

}