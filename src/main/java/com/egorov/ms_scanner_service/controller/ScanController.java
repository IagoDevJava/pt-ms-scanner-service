package com.egorov.ms_scanner_service.controller;

import com.egorov.ms_scanner_service.model.BarcodeScanRequest;
import com.egorov.ms_scanner_service.model.BarcodeScanResponse;
import com.egorov.ms_scanner_service.model.ScanRequest;
import com.egorov.ms_scanner_service.model.ScanResult;
import com.egorov.ms_scanner_service.service.CacheService;
import com.egorov.ms_scanner_service.service.ScanServiceImpl;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST-контроллер для сканирования продуктов.
 *
 * <p>Предоставляет эндпоинты для быстрого поиска по штрих-коду, запуска асинхронного
 * распознавания по фотографии и проверки статуса обработки.
 *
 * <p>Быстрое сканирование ({@code /barcode}) выполняет поиск в кэше и через
 * {@code ms-food-library}. Если продукт не найден, клиенту предлагается отправить запрос на сложное
 * сканирование.
 *
 * <p>Сложное сканирование ({@code /complex}) отправляет изображение в очередь
 * RabbitMQ для обработки сервисом {@code ms-complex-scan}. Результат возвращается асинхронно,
 * статус можно отслеживать через {@code /status/{taskId}}.
 *
 * @see ScanServiceImpl
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/scan")
@RequiredArgsConstructor
public class ScanController {

  private final CacheService cacheService;
  private final ScanServiceImpl scanOrchestratorService;

  /**
   * Поиск продукта по штрих-коду.
   *
   * <p>Сначала проверяется кэш, затем вызывается библиотека продуктов
   * ({@code ms-food-library}). В случае успеха возвращается продукт, иначе – ответ с предложением
   * сделать фото или ввести данные вручную.
   *
   * @param request объект с идентификатором пользователя и штрих-кодом, проходит валидацию через
   *                {@link Valid}
   * @return {@link ResponseEntity} с {@link BarcodeScanResponse}:
   * <ul>
   *     <li>200 OK – продукт найден</li>
   *     <li>404 Not Found – продукт не найден, дальнейшие инструкции</li>
   * </ul>
   */
  @PostMapping("/barcode")
  public ResponseEntity<BarcodeScanResponse> scanBarcode(
      @Valid @RequestBody BarcodeScanRequest request) {
    BarcodeScanResponse barcodeScanResponse = scanOrchestratorService.scanBarcode(request);
    if (barcodeScanResponse.found()) {
      return ResponseEntity.ok(barcodeScanResponse);
    } else {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(barcodeScanResponse);
    }
  }

  /**
   * Запуск сложного сканирования по фотографии продукта.
   *
   * <p>Запрос помещается в очередь RabbitMQ для обработки сервисом
   * {@code ms-complex-scan}. Текущий ответ содержит статус {@code PENDING} и идентификатор задачи
   * для отслеживания прогресса.
   *
   * @param request объект с изображением (Base64), типом сканирования и идентификатором
   *                пользователя, проходит валидацию
   * @return {@link ResponseEntity} с {@link ScanResult} в статусе {@code PENDING} и уникальным
   * {@code taskId}
   */
  @PostMapping("/complex")
  public ResponseEntity<ScanResult> complexScan(@Valid @RequestBody ScanRequest request) {
    return ResponseEntity.ok(scanOrchestratorService.complexScan(request));
  }

  /**
   * Проверка статуса задачи сложного сканирования.
   *
   * @param taskId уникальный идентификатор задачи, полученный при запуске сложного сканирования
   * @return {@link ResponseEntity} с текущим {@link ScanResult} или 404 Not Found, если задача с
   * указанным {@code taskId} не найдена
   */
  @GetMapping("/status/{taskId}")
  public ResponseEntity<ScanResult> getTaskStatus(@PathVariable UUID taskId) {
    ScanResult result = scanOrchestratorService.getTaskStatus(taskId);
    if (result == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(result);
  }
}