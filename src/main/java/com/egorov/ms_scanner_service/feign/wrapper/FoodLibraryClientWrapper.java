package com.egorov.ms_scanner_service.feign.wrapper;

import com.egorov.ms_scanner_service.feign.FoodLibraryClient;
import com.egorov.ms_scanner_service.model.ProductInfo;
import jakarta.validation.constraints.NotNull;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FoodLibraryClientWrapper {

  private final FoodLibraryClient client;


  public Optional<ProductInfo> findByBarcode(@NotNull String barcode) {
    return client.findByBarcode(barcode);
  }
}
