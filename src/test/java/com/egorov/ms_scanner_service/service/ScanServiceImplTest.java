package com.egorov.ms_scanner_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.egorov.ms_scanner_service.exception.FoodLibraryException;
import com.egorov.ms_scanner_service.exception.MinIOUploadException;
import com.egorov.ms_scanner_service.exception.QueuePublishException;
import com.egorov.ms_scanner_service.feign.wrapper.FoodLibraryClientWrapper;
import com.egorov.ms_scanner_service.model.BarcodeScanRequest;
import com.egorov.ms_scanner_service.model.BarcodeScanResponse;
import com.egorov.ms_scanner_service.model.ExternalComplexScanRequest;
import com.egorov.ms_scanner_service.model.InternalComplexScanRequest;
import com.egorov.ms_scanner_service.model.ProductInfo;
import com.egorov.ms_scanner_service.model.ScanResult;
import com.egorov.ms_scanner_service.model.ScanStatus;
import com.egorov.ms_scanner_service.producer.ScanRequestProducer;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScanServiceImplTest {

  @Mock
  private CacheService cacheService;

  @Mock
  private FoodLibraryClientWrapper foodLibraryClient;

  @Mock
  private ScanRequestProducer requestProducer;

  @InjectMocks
  private ScanServiceImpl scanService;

  @Mock
  private MinIOService minioService;

  private final ProductInfo sampleProduct = new ProductInfo(
      1L, 2L, "Молоко", "Свежее молоко 3.2%", "4601234567890",
      60.0, 3.0, 3.2, 4.7, 30, false, "Хранить при t° от +2°C до +6°C",
      BigDecimal.valueOf(2), BigDecimal.valueOf(6), 75, 14, false,
      "ext-001", null, null
  );

  // ==================== scanBarcode tests ====================

  @Test
  void shouldReturnProductFromCacheWhenPresent() {
    String barcode = "4601234567890";
    BarcodeScanRequest request = new BarcodeScanRequest(123L, barcode, null);
    when(cacheService.findByBarcode(barcode)).thenReturn(Optional.of(sampleProduct));

    BarcodeScanResponse response = scanService.scanBarcode(request);

    assertThat(response.found()).isTrue();
    assertThat(response.product().name()).isEqualTo("Молоко");
    assertThat(response.message()).contains("кеше");
    verify(foodLibraryClient, never()).findByBarcode(anyString());
  }

  @Test
  void shouldReturnProductFromLibraryAndCacheIt() {
    String barcode = "4601234567890";
    BarcodeScanRequest request = new BarcodeScanRequest(123L, barcode, null);
    when(cacheService.findByBarcode(barcode)).thenReturn(Optional.empty());
    when(foodLibraryClient.findByBarcode(barcode)).thenReturn(Optional.of(sampleProduct));

    BarcodeScanResponse response = scanService.scanBarcode(request);

    assertThat(response.found()).isTrue();
    assertThat(response.message()).contains("базе");
    verify(cacheService).put(barcode, sampleProduct);
  }

  @Test
  void shouldReturnNotFoundWhenProductNotInCacheNorLibrary() {
    String barcode = "9999999999999";
    BarcodeScanRequest request = new BarcodeScanRequest(321L, barcode, null);
    when(cacheService.findByBarcode(barcode)).thenReturn(Optional.empty());
    when(foodLibraryClient.findByBarcode(barcode)).thenReturn(Optional.empty());

    BarcodeScanResponse response = scanService.scanBarcode(request);

    assertThat(response.found()).isFalse();
    assertThat(response.message()).contains("Сфотографируйте");
  }

  @Test
  void shouldReturnNotFoundWhenLibraryServiceThrowsFoodLibraryException() {
    String barcode = "4601234567890";
    BarcodeScanRequest request = new BarcodeScanRequest(123L, barcode, null);
    when(cacheService.findByBarcode(barcode)).thenReturn(Optional.empty());
    when(foodLibraryClient.findByBarcode(barcode))
        .thenThrow(new FoodLibraryException("Service unavailable"));

    BarcodeScanResponse response = scanService.scanBarcode(request);

    assertThat(response.found()).isFalse();
    assertThat(response.message()).contains("Сфотографируйте");
    verify(cacheService, never()).put(anyString(), any());
  }

  @Test
  void shouldReturnNotFoundWhenLibraryServiceThrowsRuntimeException() {
    String barcode = "4601234567890";
    BarcodeScanRequest request = new BarcodeScanRequest(123L, barcode, null);
    when(cacheService.findByBarcode(barcode)).thenReturn(Optional.empty());
    when(foodLibraryClient.findByBarcode(barcode))
        .thenThrow(new RuntimeException("Unexpected error"));

    BarcodeScanResponse response = scanService.scanBarcode(request);

    assertThat(response.found()).isFalse();
    assertThat(response.message()).contains("Сфотографируйте");
  }

  @Test
  void shouldFallbackToLibraryWhenCacheFails() {
    String barcode = "4601234567890";
    BarcodeScanRequest request = new BarcodeScanRequest(123L, barcode, null);
    when(cacheService.findByBarcode(barcode))
        .thenThrow(new RuntimeException("Cache unavailable"));
    when(foodLibraryClient.findByBarcode(barcode)).thenReturn(Optional.of(sampleProduct));

    BarcodeScanResponse response = scanService.scanBarcode(request);

    assertThat(response.found()).isTrue();
    assertThat(response.product().name()).isEqualTo("Молоко");
  }

  // ==================== complexScan tests ====================

  @Test
  void shouldSendComplexScanRequestAndStorePendingResult() {
    UUID taskId = UUID.randomUUID();
    String fakeUrl = "http://minio/product-scans-test/" + taskId + ".jpg";
    ExternalComplexScanRequest externalRequest = new ExternalComplexScanRequest(
        taskId, 1L, "base64", null, null);

    when(minioService.uploadImage(anyString(), any(UUID.class))).thenReturn(fakeUrl);

    ScanResult result = scanService.complexScan(externalRequest);

    assertThat(result.status()).isEqualTo(ScanStatus.PENDING);
    assertThat(result.taskId()).isEqualTo(taskId);

    // Проверяем, что в очередь отправился InternalComplexScanRequest с правильным URL
    ArgumentCaptor<InternalComplexScanRequest> captor = ArgumentCaptor.forClass(
        InternalComplexScanRequest.class);
    verify(requestProducer).sendRequest(captor.capture());
    InternalComplexScanRequest sent = captor.getValue();
    assertThat(sent.imageUrl()).isEqualTo(fakeUrl);
    assertThat(sent.taskId()).isEqualTo(taskId);
    assertThat(sent.userId()).isEqualTo(1L);

    verify(cacheService).putScanResult(eq(taskId), any(ScanResult.class));
  }

  @Test
  void shouldReturnFailedWhenQueuePublishException() {
    UUID taskId = UUID.randomUUID();
    String fakeUrl = "http://minio/product-scans-test/" + taskId + ".jpg";
    ExternalComplexScanRequest externalRequest = new ExternalComplexScanRequest(
        taskId, 1L, "base64", null, null);

    when(minioService.uploadImage(anyString(), any(UUID.class))).thenReturn(fakeUrl);
    doThrow(new QueuePublishException(taskId, "RabbitMQ unavailable"))
        .when(requestProducer).sendRequest(any());

    ScanResult result = scanService.complexScan(externalRequest);

    assertThat(result.status()).isEqualTo(ScanStatus.FAILED);
    assertThat(result.message()).contains("временно недоступен");
    verify(cacheService, never()).putScanResult(any(), any());
  }

  @Test
  void shouldReturnFailedWhenUnexpectedException() {
    UUID taskId = UUID.randomUUID();
    String fakeUrl = "http://minio/product-scans-test/" + taskId + ".jpg";
    ExternalComplexScanRequest externalRequest = new ExternalComplexScanRequest(
        taskId, 1L, "base64", null, null);

    when(minioService.uploadImage(anyString(), any(UUID.class))).thenReturn(fakeUrl);
    doThrow(new RuntimeException("Unexpected error"))
        .when(requestProducer).sendRequest(any());

    ScanResult result = scanService.complexScan(externalRequest);

    assertThat(result.status()).isEqualTo(ScanStatus.FAILED);
    assertThat(result.message()).contains("Непредвиденная ошибка");
  }

  @Test
  void shouldReturnFailedWhenMinIoUploadFails() {
    UUID taskId = UUID.randomUUID();
    ExternalComplexScanRequest externalRequest = new ExternalComplexScanRequest(
        taskId, 1L, "base64", null, null);

    when(minioService.uploadImage(anyString(), any(UUID.class)))
        .thenThrow(new MinIOUploadException("Upload failed"));

    ScanResult result = scanService.complexScan(externalRequest);

    assertThat(result.status()).isEqualTo(ScanStatus.FAILED);
    assertThat(result.message()).contains("Не удалось загрузить изображение");
    verify(requestProducer, never()).sendRequest(any());
    verify(cacheService, never()).putScanResult(any(), any());
  }

  @Test
  void shouldReturnFailedWhenCacheFailsAfterSuccessfulSend() {
    UUID taskId = UUID.randomUUID();
    String fakeUrl = "http://minio/product-scans-test/" + taskId + ".jpg";
    ExternalComplexScanRequest externalRequest = new ExternalComplexScanRequest(
        taskId, 1L, "base64", null, null);

    when(minioService.uploadImage(anyString(), any(UUID.class))).thenReturn(fakeUrl);
    doThrow(new RuntimeException("Cache write error"))
        .when(cacheService).putScanResult(eq(taskId), any());

    ScanResult result = scanService.complexScan(externalRequest);

    assertThat(result.status()).isEqualTo(ScanStatus.FAILED);
    assertThat(result.message()).contains("Непредвиденная ошибка");
    verify(requestProducer).sendRequest(any()); // отправка всё равно произошла
  }

  // ==================== updateResult tests ====================

  @Test
  void shouldUpdateResultInCache() {
    UUID taskId = UUID.randomUUID();
    ScanResult result = ScanResult.success(taskId, 1L, sampleProduct, 500L);

    scanService.updateResult(result);

    verify(cacheService).putScanResult(taskId, result);
  }

  @Test
  void shouldNotUpdateResultWhenTaskIdIsNull() {
    ScanResult result = ScanResult.success(null, 1L, sampleProduct, 500L);

    scanService.updateResult(result);

    verify(cacheService, never()).putScanResult(any(), any());
  }

  @Test
  void shouldNotUpdateResultWhenResultIsNull() {
    scanService.updateResult(null);

    verify(cacheService, never()).putScanResult(any(), any());
  }

  @Test
  void shouldHandleCacheExceptionGracefully() {
    UUID taskId = UUID.randomUUID();
    ScanResult result = ScanResult.success(taskId, 1L, sampleProduct, 500L);
    doThrow(new RuntimeException("Cache write error"))
        .when(cacheService).putScanResult(taskId, result);

    scanService.updateResult(result);

    verify(cacheService).putScanResult(taskId, result);
  }

  // ==================== getTaskStatus tests ====================

  @Test
  void shouldGetTaskStatusFromCache() {
    UUID taskId = UUID.randomUUID();
    ScanResult expected = ScanResult.processing(taskId, 1L);
    when(cacheService.getTaskStatus(taskId)).thenReturn(expected);

    ScanResult actual = scanService.getTaskStatus(taskId);

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  void shouldReturnNullWhenTaskIdIsNull() {
    ScanResult result = scanService.getTaskStatus(null);

    assertThat(result).isNull();
  }

  @Test
  void shouldReturnNullWhenCacheThrowsException() {
    UUID taskId = UUID.randomUUID();
    when(cacheService.getTaskStatus(taskId))
        .thenThrow(new RuntimeException("Cache read error"));

    ScanResult result = scanService.getTaskStatus(taskId);

    assertThat(result).isNull();
  }

  // ==================== integration-like tests ====================

  @Test
  void shouldReturnProductWithoutCachingWhenCachePutFails() {
    String barcode = "4601234567890";
    BarcodeScanRequest request = new BarcodeScanRequest(123L, barcode, null);
    when(cacheService.findByBarcode(barcode)).thenReturn(Optional.empty());
    when(foodLibraryClient.findByBarcode(barcode)).thenReturn(Optional.of(sampleProduct));
    doThrow(new RuntimeException("Cache write error"))
        .when(cacheService).put(barcode, sampleProduct);

    BarcodeScanResponse response = scanService.scanBarcode(request);

    assertThat(response.found()).isTrue();
    assertThat(response.product().name()).isEqualTo("Молоко");
  }
}