package com.egorov.ms_scanner_service.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import java.time.ZonedDateTime;
import java.util.UUID;

public record ExternalComplexScanRequest(

    @NotNull
    UUID taskId,
    @NotNull
    Long userId,
    @NotNull
    String imageBase64,
    ScanType scanType,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    ZonedDateTime timestamp
) {

  public ExternalComplexScanRequest {
    if (taskId == null) {
      taskId = UUID.randomUUID();
    }
    if (timestamp == null) {
      timestamp = ZonedDateTime.now();
    }
    if (scanType == null) {
      scanType = ScanType.AUTO;
    }
  }
}
