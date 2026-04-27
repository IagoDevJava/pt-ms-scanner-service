package com.egorov.ms_scanner_service.feign;

import com.egorov.ms_scanner_service.model.ProductInfo;
import jakarta.validation.constraints.NotNull;
import java.util.Optional;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient("ms-food-categories")
public class FoodLibraryClient {
  // TODO implements Api

  public Optional<ProductInfo> findByBarcode(@NotNull String barcode) {
    return null;
  }
}
