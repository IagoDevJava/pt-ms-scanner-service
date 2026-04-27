package com.egorov.ms_scanner_service.model;

public record BarcodeScanResponse(
    boolean found,
    ProductInfo product,
    String message,
    String barcode,
    long processingTimeMs
) {

  public static BarcodeScanResponse found(
      ProductInfo product,
      String message,
      String barcode,
      long processingTimeMs) {
    return new BarcodeScanResponse(true, product, message, barcode, processingTimeMs);
  }

  public static BarcodeScanResponse notFound(
      String barcode,
      String message,
      long processingTimeMs) {
    return new BarcodeScanResponse(false, null, message, barcode, processingTimeMs);
  }
}
