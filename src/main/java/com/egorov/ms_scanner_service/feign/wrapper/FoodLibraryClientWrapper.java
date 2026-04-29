package com.egorov.ms_scanner_service.feign.wrapper;

import com.egorov.ms_scanner_service.exception.FoodLibraryException;
import com.egorov.ms_scanner_service.feign.FoodLibraryClient;
import com.egorov.ms_scanner_service.model.ProductInfo;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FoodLibraryClientWrapper {

  private final FoodLibraryClient client;

  @CircuitBreaker(name = "foodLibrary", fallbackMethod = "findByBarcodeFallback")
  public Optional<ProductInfo> findByBarcode(String barcode) {
    try {
      return client.findByBarcode(barcode);
    } catch (Exception e) {
      throw new FoodLibraryException(
          "Failed to call food library service for barcode: " + barcode, e
      );
    }
  }

  private Optional<ProductInfo> findByBarcodeFallback(String barcode, Throwable t) {
    log.error("CircuitBreaker fallback for barcode {}. Service unavailable: {}",
        barcode, t.getMessage());

    if (t instanceof FoodLibraryException) {
      log.debug("FoodLibraryException caught in fallback", t);
    }

    return Optional.empty();
  }
}