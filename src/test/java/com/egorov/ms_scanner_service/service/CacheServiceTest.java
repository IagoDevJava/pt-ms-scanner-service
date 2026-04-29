package com.egorov.ms_scanner_service.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.egorov.ms_scanner_service.model.ProductInfo;
import com.egorov.ms_scanner_service.model.ScanResult;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class CacheServiceTest {

  private CacheService cacheService;

  @BeforeEach
  void setUp() {
    cacheService = new CacheService();
    ReflectionTestUtils.setField(cacheService, "barcodeMaxSize", 100);
    ReflectionTestUtils.setField(cacheService, "barcodeTtlHours", 1);
    ReflectionTestUtils.setField(cacheService, "scanResultMaxSize", 100);
    ReflectionTestUtils.setField(cacheService, "scanResultTtlMinutes", 1);
    cacheService.init();
  }

  @Test
  void shouldStoreAndRetrieveProductByBarcode() {
    ProductInfo product = new ProductInfo(1L, null, "Тест", "", "111", null, null, null, null,
        null, null, null, null, null, null, null, null, null, null, null);
    cacheService.put("111", product);
    assertThat(cacheService.findByBarcode("111")).contains(product);
    assertThat(cacheService.findByBarcode("222")).isEmpty();
  }

  @Test
  void shouldStoreScanResultAndUpdateBarcodeCacheOnCompletion() {
    UUID taskId = UUID.randomUUID();
    ProductInfo product = new ProductInfo(2L, null, "Товар", "", "222", null, null, null, null,
        null, null, null, null, null, null, null, null, null, null, null);
    ScanResult success = ScanResult.success(taskId, 1L, product, 100L);

    cacheService.putScanResult(taskId, success);
    assertThat(cacheService.getTaskStatus(taskId)).isEqualTo(success);
    assertThat(cacheService.findByBarcode("222")).contains(product);
  }

  @Test
  void shouldNotStoreBarcodeWhenResultNotCompleted() {
    UUID taskId = UUID.randomUUID();
    ScanResult pending = ScanResult.pending(taskId, 1L);
    cacheService.putScanResult(taskId, pending);
    assertThat(cacheService.findByBarcode("неважно")).isEmpty();
  }
}