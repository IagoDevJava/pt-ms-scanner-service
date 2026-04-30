package com.egorov.ms_scanner_service.exception;

public class MinIOUploadException extends RuntimeException {

  public MinIOUploadException(String message) {
    super(message);
  }

  public MinIOUploadException(String message, Throwable cause) {
    super(message, cause);
  }
}
