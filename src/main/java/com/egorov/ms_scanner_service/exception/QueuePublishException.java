package com.egorov.ms_scanner_service.exception;

import java.util.UUID;

/**
 * Исключение при отправке запроса в очередь RabbitMQ.
 */
public class QueuePublishException extends ComplexScanException {

  public QueuePublishException(UUID taskId, String message) {
    super(taskId, message);
  }

  public QueuePublishException(UUID taskId, String message, Throwable cause) {
    super(taskId, message, cause);
  }
}