package com.egorov.ms_scanner_service.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.ZonedDateTime;
import java.util.UUID;

public record InternalComplexScanRequest(
    @NotNull UUID taskId,           // уникальный идентификатор задачи
    @NotBlank String imageUrl,      // URL в MinIO
    @NotNull ScanType scanType,     // BARCODE, OCR, ML, AUTO
    @NotNull Long userId,           // для логирования
    UUID traceId,                  // для сквозной трассировки
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    ZonedDateTime timestamp
) {

  public InternalComplexScanRequest {
    if (taskId == null) {
      taskId = UUID.randomUUID();
    }
    if (traceId == null) {
      traceId = UUID.randomUUID();
    }
    if (timestamp == null) {
      timestamp = ZonedDateTime.now();
    }
    if (scanType == null) {
      scanType = ScanType.AUTO;
    }
  }
}