package com.egorov.ms_scanner_service.model;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record BarcodeScanRequest(
    @NotNull Long userId,
    @NotNull String barcode,
    LocalDateTime timestamp
) {

  public BarcodeScanRequest {
    if (timestamp == null) {
      timestamp = LocalDateTime.now();
    }
  }
}
