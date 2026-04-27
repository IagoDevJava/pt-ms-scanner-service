package com.egorov.ms_scanner_service.model;

import java.time.ZonedDateTime;
import java.util.UUID;

public record ScanNotificationEvent(
    UUID taskId,
    Long userId,
    ScanStatus status,
    ProductInfo product,
    String message,
    Long processingTimeMs,
    ZonedDateTime completedAt,
    NotificationType notificationType
) {

  public static ScanNotificationEvent from(ScanResult result, NotificationType type) {
    return new ScanNotificationEvent(
        result.taskId(),
        result.userId(),
        result.status(),
        result.product(),
        result.message(),
        result.processingTimeMs(),
        result.completedAt(),
        type
    );
  }

  public enum NotificationType {
    SCAN_COMPLETED,
    SCAN_FAILED,
    RESULT_UPDATED
  }
}