package com.egorov.ms_scanner_service.service;

import com.egorov.ms_scanner_service.exception.FoodLibraryException;
import com.egorov.ms_scanner_service.exception.MinIOUploadException;
import com.egorov.ms_scanner_service.exception.QueuePublishException;
import com.egorov.ms_scanner_service.feign.wrapper.FoodLibraryClientWrapper;
import com.egorov.ms_scanner_service.mapper.ComplexScanRequestMapper;
import com.egorov.ms_scanner_service.model.BarcodeScanRequest;
import com.egorov.ms_scanner_service.model.BarcodeScanResponse;
import com.egorov.ms_scanner_service.model.ExternalComplexScanRequest;
import com.egorov.ms_scanner_service.model.InternalComplexScanRequest;
import com.egorov.ms_scanner_service.model.ProductInfo;
import com.egorov.ms_scanner_service.model.ScanResult;
import com.egorov.ms_scanner_service.producer.ScanRequestProducer;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScanServiceImpl implements ScanService {

  private final CacheService cacheService;
  private final FoodLibraryClientWrapper foodLibraryClient;
  private final ScanRequestProducer requestProducer;
  private final MinIOService minIOService;

  @Override
  public BarcodeScanResponse scanBarcode(BarcodeScanRequest request) {
    long startTime = System.currentTimeMillis();

    log.info("Barcode scan: userId={}, barcode={}", request.userId(), request.barcode());

    // 1. Проверяем кэш
    try {
      Optional<ProductInfo> cachedProduct = cacheService.findByBarcode(request.barcode());
      if (cachedProduct.isPresent()) {
        long duration = System.currentTimeMillis() - startTime;
        log.info("Barcode scan CACHE HIT: {} ms", duration);
        return BarcodeScanResponse.found(
            cachedProduct.get(),
            "Продукт найден в кеше",
            request.barcode(),
            duration
        );
      }
    } catch (Exception e) {
      log.warn(
          "Cache lookup failed for barcode {}: {}",
          request.barcode(), e.getMessage()
      );
    }

    // 2. Ищем в библиотеке продуктов
    try {
      Optional<ProductInfo> product = foodLibraryClient.findByBarcode(request.barcode());
      if (product.isPresent()) {
        try {
          cacheService.put(request.barcode(), product.get());
        } catch (Exception e) {
          log.warn(
              "Failed connect to food library client for barcode {}: {}",
              request.barcode(), e.getMessage()
          );
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("Barcode scan LIBRARY HIT: {} ms", duration);
        return BarcodeScanResponse.found(
            product.get(),
            "Продукт найден в базе",
            request.barcode(),
            duration
        );
      }
    } catch (FoodLibraryException e) {
      long duration = System.currentTimeMillis() - startTime;
      log.error(
          "Barcode scan FAILED (FoodLibraryException): barcode={}, {} ms, error: {}",
          request.barcode(), duration, e.getMessage(), e
      );
    } catch (Exception e) {
      long duration = System.currentTimeMillis() - startTime;
      log.error(
          "Barcode scan FAILED (Unexpected): barcode={}, {} ms, error: {}",
          request.barcode(), duration, e.getMessage(), e
      );
    }

    long duration = System.currentTimeMillis() - startTime;
    log.info("Barcode scan NOT FOUND: {} ms", duration);
    return BarcodeScanResponse.notFound(
        request.barcode(),
        "Продукт не найден. Сфотографируйте упаковку или добавьте вручную.",
        duration
    );
  }

  @Override
  public ScanResult complexScan(ExternalComplexScanRequest extRequest) {
    log.info(
        "Complex scan request: userId={}, taskId={}",
        extRequest.userId(), extRequest.taskId()
    );

    String imageUrl;

    try {
      imageUrl = minIOService.uploadImage(extRequest.imageBase64(), extRequest.taskId());
    } catch (MinIOUploadException e) {
      log.error("MinIO upload failed: taskId={}", extRequest.taskId(), e);
      return ScanResult.failed(
          extRequest.taskId(),
          extRequest.userId(),
          "Не удалось загрузить изображение. Попробуйте позже.",
          0L
      );
    }

    InternalComplexScanRequest intRequest = ComplexScanRequestMapper.toInternalRequest(
        extRequest, imageUrl);

    try {
      requestProducer.sendRequest(intRequest);

      ScanResult pendingResult = ScanResult.pending(intRequest.taskId(), intRequest.userId());
      cacheService.putScanResult(intRequest.taskId(), pendingResult);

      return pendingResult;
    } catch (QueuePublishException e) {
      log.error(
          "Failed to publish scan request to queue: taskId={}, error={}",
          e.getTaskId(), e.getMessage(), e
      );

      return ScanResult.failed(
          e.getTaskId(),
          intRequest.userId(),
          "Сервис сканирования временно недоступен. Попробуйте позже.",
          0L
      );
    } catch (Exception e) {
      log.error(
          "Unexpected exception during complex scan: taskId={}, error={}",
          intRequest.taskId(), e.getMessage(), e
      );

      return ScanResult.failed(
          intRequest.taskId(),
          intRequest.userId(),
          "Непредвиденная ошибка сканирования. Попробуйте позже.",
          0L
      );
    }
  }

  public void updateResult(ScanResult result) {
    if (result == null) {
      log.warn("Attempt to update null scan result");
      return;
    }
    if (result.taskId() == null) {
      log.warn(
          "Attempt to update scan result with null taskId: status={}",
          result.status()
      );
      return;
    }

    log.info(
        "Updating scan result: taskId={}, status={}",
        result.taskId(), result.status()
    );

    try {
      cacheService.putScanResult(result.taskId(), result);
    } catch (RuntimeException e) {
      log.error(
          "Failed to update scan result in cache: taskId={}, error={}",
          result.taskId(), e.getMessage(), e
      );
    }
  }

  public ScanResult getTaskStatus(UUID taskId) {
    if (taskId == null) {
      log.warn("Attempt to get task status with null taskId");
      return null;
    }

    try {
      return cacheService.getTaskStatus(taskId);
    } catch (RuntimeException e) {
      log.error(
          "Failed to get task status from cache: taskId={}, error={}",
          taskId, e.getMessage(), e
      );
      return null;
    }
  }
}