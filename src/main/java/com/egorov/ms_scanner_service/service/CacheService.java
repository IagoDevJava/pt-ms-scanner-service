package com.egorov.ms_scanner_service.service;

import com.egorov.ms_scanner_service.model.ProductInfo;
import com.egorov.ms_scanner_service.model.ScanResult;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Сервис кэширования на основе Caffeine.
 *
 * <p>Содержит два независимых кэша:
 * <ul>
 *   <li><b>Штрих-кодов</b> — {@link ProductInfo} по {@code barcode}, TTL 24 часа</li>
 *   <li><b>Результатов сканирования</b> — {@link ScanResult} по {@code taskId}, TTL 10 минут</li>
 * </ul>
 *
 * <p>При сохранении успешного результата продукт автоматически попадает в кэш штрих-кодов.
 */
@Slf4j
@Service
public class CacheService {

  private Cache<String, ProductInfo> barcodeCache;
  private Cache<UUID, ScanResult> scanResultCache;

  @Value("${barcode.cache.max-size:100000}")
  private int barcodeMaxSize;

  @Value("${barcode.cache.ttl-hours:24}")
  private int barcodeTtlHours;

  @Value("${scan-result.cache.max-size:10000}")
  private int scanResultMaxSize;

  @Value("${scan-result.cache.ttl-minutes:10}")
  private int scanResultTtlMinutes;

  @PostConstruct
  public void init() {
    this.barcodeCache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofHours(barcodeTtlHours))
        .maximumSize(barcodeMaxSize)
        .recordStats()
        .removalListener(
            (key, value, cause) -> log.debug("Barcode cache entry removed - barcode: {}, cause: {}",
                key, cause))
        .build();

    this.scanResultCache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(scanResultTtlMinutes))
        .maximumSize(scanResultMaxSize)
        .recordStats()
        .removalListener((key, value, cause) -> log.debug(
            "Scan result cache entry removed - taskId: {}, cause: {}", key, cause))
        .build();

    log.info(
        "CacheService initialized - barcodeCache maxSize: {}, ttl: {} hours, scanResultCache maxSize: {}, ttl: {} minutes",
        barcodeMaxSize, barcodeTtlHours, scanResultMaxSize, scanResultTtlMinutes);
  }

  /**
   * Поиск продукта по штрих-коду в кеше
   */
  public Optional<ProductInfo> findByBarcode(String barcode) {
    ProductInfo product = barcodeCache.getIfPresent(barcode);

    if (product != null) {
      log.debug("Barcode cache HIT: {}", barcode);
      return Optional.of(product);
    }

    log.debug("Barcode cache MISS: {}", barcode);
    return Optional.empty();
  }

  /**
   * Сохранение продукта в кеш по штрих-коду
   */
  public void put(String barcode, ProductInfo product) {
    if (barcode == null || product == null) {
      log.warn("Attempt to cache null barcode or product: barcode={}, product={}", barcode,
          product);
      return;
    }
    barcodeCache.put(barcode, product);
    log.debug("Barcode cached: {} -> {}", barcode, product.name());
  }

  /**
   * Сохранение результата сканирования в кеш
   */
  public void putScanResult(UUID taskId, ScanResult result) {
    if (taskId == null || result == null) {
      log.warn("Attempt to cache null taskId or result: taskId={}, result={}", taskId, result);
      return;
    }
    scanResultCache.put(taskId, result);
    log.debug("Scan result cached: taskId={}, status={}", taskId, result.status());

    if (result.status() == com.egorov.ms_scanner_service.model.ScanStatus.COMPLETED
        && result.product() != null
        && result.product().barcode() != null) {
      put(result.product().barcode(), result.product());
    }
  }

  /**
   * Получение статуса задачи (удобный метод для контроллера)
   */
  public ScanResult getTaskStatus(UUID taskId) {
    return scanResultCache.getIfPresent(taskId);
  }
}