package com.egorov.ms_scanner_service.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.egorov.ms_scanner_service.config.RabbitMQConfig;
import com.egorov.ms_scanner_service.feign.FoodLibraryClient;
import com.egorov.ms_scanner_service.model.ProductInfo;
import com.egorov.ms_scanner_service.model.ScanResult;
import com.egorov.ms_scanner_service.model.ScanStatus;
import com.egorov.ms_scanner_service.service.CacheService;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class ScanResultConsumerTest {

  @Autowired
  private ScanResultConsumer scanResultConsumer;

  @Autowired
  private CacheService cacheService;

  @MockitoBean
  private RabbitTemplate rabbitTemplate;

  @MockitoBean
  private FoodLibraryClient foodLibraryClient;

  private final ProductInfo sampleProduct = new ProductInfo(
      10L, 5L, "Хлеб", "Белый хлеб", "1234567890123",
      265.0, 7.6, 3.2, 49.0, 70, true, "Сухое место",
      BigDecimal.ONE, BigDecimal.TEN, 50, 3, false, "ext-bread", null, null
  );

  @Test
  void shouldUpdateCacheAndSendNotificationOnCompletedResult() {
    UUID taskId = UUID.randomUUID();
    ScanResult completedResult = ScanResult.success(taskId, 1L, sampleProduct, 500L);

    scanResultConsumer.consumeScanResult(completedResult);

    ScanResult cached = cacheService.getTaskStatus(taskId);
    assertThat(cached).isNotNull();
    assertThat(cached.status()).isEqualTo(ScanStatus.COMPLETED);
    assertThat(cached.product().name()).isEqualTo("Хлеб");

    assertThat(cacheService.findByBarcode("1234567890123")).isPresent();

    verify(rabbitTemplate).convertAndSend(
        eq(RabbitMQConfig.SCAN_EXCHANGE),
        eq(RabbitMQConfig.SCAN_NOTIFICATION_RK),
        any(Object.class),
        any(MessagePostProcessor.class)
    );
  }

  @Test
  void shouldHandleFailedResult() {
    UUID taskId = UUID.randomUUID();
    ScanResult failedResult = ScanResult.failed(taskId, 1L, "Ошибка распознавания", 1200L);

    scanResultConsumer.consumeScanResult(failedResult);

    ScanResult cached = cacheService.getTaskStatus(taskId);
    assertThat(cached).isNotNull();
    assertThat(cached.status()).isEqualTo(ScanStatus.FAILED);
    assertThat(cached.message()).isEqualTo("Ошибка распознавания");
  }
}