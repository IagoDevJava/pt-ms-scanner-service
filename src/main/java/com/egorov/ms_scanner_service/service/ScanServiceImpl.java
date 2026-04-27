package com.egorov.ms_scanner_service.service;

import com.egorov.ms_scanner_service.consumer.ScanResultConsumer;
import com.egorov.ms_scanner_service.feign.wrapper.FoodLibraryClientWrapper;
import com.egorov.ms_scanner_service.model.BarcodeScanRequest;
import com.egorov.ms_scanner_service.model.BarcodeScanResponse;
import com.egorov.ms_scanner_service.model.ProductInfo;
import com.egorov.ms_scanner_service.model.ScanRequest;
import com.egorov.ms_scanner_service.model.ScanResult;
import com.egorov.ms_scanner_service.producer.ScanRequestProducer;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Сервис-оркестратор сканирования продуктов.
 *
 * <p>Реализует {@link ScanService}. Выполняет поиск по штрих-коду через кэш и
 * библиотеку продуктов, а также запускает асинхронное распознавание по изображению через очередь
 * RabbitMQ.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScanServiceImpl implements ScanService {

  private final CacheService cacheService;
  private final FoodLibraryClientWrapper foodLibraryClient;
  private final ScanRequestProducer requestProducer;

  /**
   * Поиск продукта по штрих-коду.
   *
   * <p>Порядок проверки:
   * <ol>
   *   <li>Локальный кэш ({@link CacheService#findByBarcode})</li>
   *   <li>Внешний сервис библиотеки продуктов через Feign</li>
   * </ol>
   *
   * @return {@link BarcodeScanResponse} с флагом {@code found} и продуктом, либо инструкцией для
   * цией для ручного ввода/фотографирования
   */
  @Override
  public BarcodeScanResponse scanBarcode(BarcodeScanRequest request) {
    long startTime = System.currentTimeMillis();

    log.info("Barcode scan: userId={}, barcode={}", request.userId(), request.barcode());

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

    Optional<ProductInfo> product = foodLibraryClient.findByBarcode(request.barcode());

    if (product.isPresent()) {
      cacheService.put(request.barcode(), product.get());

      long duration = System.currentTimeMillis() - startTime;
      log.info("Barcode scan LIBRARY HIT: {} ms", duration);

      return BarcodeScanResponse.found(
          product.get(),
          "Продукт найден в базе",
          request.barcode(),
          duration
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

  /**
   * Запускает асинхронное сканирование изображения.
   *
   * <p>Отправляет запрос в очередь RabbitMQ и сохраняет начальный статус
   * {@code PENDING} в кэше результатов.
   *
   * @return {@link ScanResult} с уникальным {@code taskId} для отслеживания
   */
  @Override
  public ScanResult complexScan(ScanRequest request) {
    log.info("Complex scan request: userId={}, taskId={}",
        request.userId(), request.taskId());

    requestProducer.sendRequest(request);

    ScanResult pendingResult = ScanResult.pending(request.taskId(), request.userId());
    cacheService.putScanResult(request.taskId(), pendingResult);

    return pendingResult;
  }

  /**
   * Обновляет результат сканирования в кэше.
   *
   * <p>Вызывается из {@link ScanResultConsumer} при получении ответа от
   * {@code ms-complex-scan}. При успешном завершении также обновляет кэш продуктов.
   */
  public void updateResult(ScanResult result) {
    if (result != null && result.taskId() != null) {
      log.info("Updating scan result: taskId={}, status={}", result.taskId(), result.status());
      cacheService.putScanResult(result.taskId(), result);
    }
  }

  /**
   * Возвращает статус асинхронной задачи по её идентификатору.
   *
   * @param taskId идентификатор задачи из {@link #complexScan(ScanRequest)}
   * @return текущий {@link ScanResult} или {@code null}, если задача не найдена
   */
  public ScanResult getTaskStatus(UUID taskId) {
    return cacheService.getTaskStatus(taskId);
  }
}