package com.egorov.ms_scanner_service.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.ZonedDateTime;
import java.util.UUID;

public record ScanResult(
    UUID taskId,
    Long userId,
    ScanStatus status,
    ProductInfo product,
    String message,
    Long processingTimeMs,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    ZonedDateTime completedAt
) {

  public static ScanResult success(UUID taskId, Long userId, ProductInfo product,
      Long processingTimeMs) {
    return new ScanResult(taskId, userId, ScanStatus.COMPLETED, product, null,
        processingTimeMs, ZonedDateTime.now());
  }

  public static ScanResult failed(UUID taskId, Long userId,
      Long processingTimeMs) {
    return new ScanResult(taskId, userId, ScanStatus.FAILED, null,
        "По Вашему фото товар не найден", processingTimeMs, ZonedDateTime.now());
  }

  public static ScanResult failed(UUID taskId, Long userId, String errorMessage,
      Long processingTimeMs) {
    return new ScanResult(taskId, userId, ScanStatus.FAILED, null,
        errorMessage, processingTimeMs, ZonedDateTime.now());
  }

  public static ScanResult retry(UUID taskId, Long userId) {
    return new ScanResult(taskId, userId, ScanStatus.RETRY, null,
        "Необходима повторная попытка", null, ZonedDateTime.now());
  }

  public static ScanResult pending(UUID taskId, Long userId) {
    return new ScanResult(taskId, userId, ScanStatus.PENDING, null,
        "Ожидайте, идёт обработка.", null, ZonedDateTime.now());
  }

  public static ScanResult processing(UUID taskId, Long userId) {
    return new ScanResult(taskId, userId, ScanStatus.PROCESSING, null,
        "Фото отправлено на обработку. Мы уведомим вас о результате.",
        null, ZonedDateTime.now());
  }
}