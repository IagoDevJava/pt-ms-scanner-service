package com.egorov.ms_scanner_service.feign;

import com.egorov.ms_scanner_service.model.ProductInfo;
import jakarta.validation.constraints.NotNull;
import java.util.Optional;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "ms-food-categories", url = "${food-categories.url}")
public interface FoodLibraryClient {
  // TODO implements Api

  @GetMapping("/api/v1/products/barcode/{barcode}")
  Optional<ProductInfo> findByBarcode(@NotNull String barcode);
}
