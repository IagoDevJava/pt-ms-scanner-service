package com.egorov.ms_scanner_service.producer;

import static com.egorov.ms_scanner_service.config.RabbitMQConfig.SCAN_EXCHANGE;
import static com.egorov.ms_scanner_service.config.RabbitMQConfig.SCAN_NOTIFICATION_RK;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.egorov.ms_scanner_service.model.ProductInfo;
import com.egorov.ms_scanner_service.model.ScanNotificationEvent;
import com.egorov.ms_scanner_service.model.ScanResult;
import com.egorov.ms_scanner_service.model.ScanStatus;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@ExtendWith(MockitoExtension.class)
class ScanNotificationProducerTest {

  @Mock
  private RabbitTemplate rabbitTemplate;

  @InjectMocks
  private ScanNotificationProducer scanNotificationProducer;

  private final UUID taskId = UUID.randomUUID();

  private final ProductInfo sampleProduct = new ProductInfo(
      1L, 2L, "Тестовый продукт", "Описание", "123456789",
      100.0, 5.0, 3.0, 15.0, 50, false, "Холодильник",
      BigDecimal.ONE, BigDecimal.TEN, 60, 7, false,
      "ext-1", null, null
  );

  @Test
  void shouldSendCompletedNotification() {
    ScanResult result = ScanResult.success(taskId, 1L, sampleProduct, 500L);

    scanNotificationProducer.sendScanCompletedNotification(result);

    ArgumentCaptor<ScanNotificationEvent> eventCaptor =
        ArgumentCaptor.forClass(ScanNotificationEvent.class);

    verify(rabbitTemplate).convertAndSend(
        eq(SCAN_EXCHANGE),
        eq(SCAN_NOTIFICATION_RK),
        eventCaptor.capture(),
        any(MessagePostProcessor.class)
    );

    ScanNotificationEvent event = eventCaptor.getValue();
    assertThat(event.taskId()).isEqualTo(taskId);
    assertThat(event.notificationType())
        .isEqualTo(ScanNotificationEvent.NotificationType.SCAN_COMPLETED);
    assertThat(event.status()).isEqualTo(ScanStatus.COMPLETED);
  }

  @Test
  void shouldSendFailedNotification() {
    ScanResult result = ScanResult.failed(taskId, 1L, "Ошибка обработки", 200L);

    scanNotificationProducer.sendScanCompletedNotification(result);

    ArgumentCaptor<ScanNotificationEvent> eventCaptor =
        ArgumentCaptor.forClass(ScanNotificationEvent.class);

    verify(rabbitTemplate).convertAndSend(
        eq(SCAN_EXCHANGE),
        eq(SCAN_NOTIFICATION_RK),
        eventCaptor.capture(),
        any(MessagePostProcessor.class)
    );

    ScanNotificationEvent event = eventCaptor.getValue();
    assertThat(event.taskId()).isEqualTo(taskId);
    assertThat(event.notificationType())
        .isEqualTo(ScanNotificationEvent.NotificationType.SCAN_FAILED);
    assertThat(event.status()).isEqualTo(ScanStatus.FAILED);
  }

  @Test
  void shouldSendResultUpdatedNotification() {
    ScanResult result = ScanResult.processing(taskId, 1L);

    scanNotificationProducer.sendResultUpdatedNotification(result);

    ArgumentCaptor<ScanNotificationEvent> eventCaptor =
        ArgumentCaptor.forClass(ScanNotificationEvent.class);

    verify(rabbitTemplate).convertAndSend(
        eq(SCAN_EXCHANGE),
        eq(SCAN_NOTIFICATION_RK),
        eventCaptor.capture(),
        any(MessagePostProcessor.class)
    );

    ScanNotificationEvent event = eventCaptor.getValue();
    assertThat(event.taskId()).isEqualTo(taskId);
    assertThat(event.notificationType())
        .isEqualTo(ScanNotificationEvent.NotificationType.RESULT_UPDATED);
    assertThat(event.status()).isEqualTo(ScanStatus.PROCESSING);
  }

  @Test
  void shouldSetHighPriorityForCompletedStatus() {
    ScanResult result = ScanResult.success(taskId, 1L, sampleProduct, 100L);

    scanNotificationProducer.sendScanCompletedNotification(result);

    ArgumentCaptor<MessagePostProcessor> processorCaptor =
        ArgumentCaptor.forClass(MessagePostProcessor.class);

    verify(rabbitTemplate).convertAndSend(
        eq(SCAN_EXCHANGE),
        eq(SCAN_NOTIFICATION_RK),
        any(ScanNotificationEvent.class),
        processorCaptor.capture()
    );

    MessagePostProcessor processor = processorCaptor.getValue();
    assertThat(processor).isNotNull();
  }

  @Test
  void shouldSetLowerPriorityForNonCompletedStatus() {
    ScanResult result = ScanResult.failed(taskId, 1L, "Error", 0L);

    scanNotificationProducer.sendScanCompletedNotification(result);

    ArgumentCaptor<MessagePostProcessor> processorCaptor =
        ArgumentCaptor.forClass(MessagePostProcessor.class);

    verify(rabbitTemplate).convertAndSend(
        eq(SCAN_EXCHANGE),
        eq(SCAN_NOTIFICATION_RK),
        any(ScanNotificationEvent.class),
        processorCaptor.capture()
    );

    MessagePostProcessor processor = processorCaptor.getValue();
    assertThat(processor).isNotNull();
  }

  @Test
  void shouldSurviveRabbitExceptionWhenSendingNotification() {
    ScanResult result = ScanResult.success(taskId, 1L, sampleProduct, 500L);

    doThrow(new RuntimeException("RabbitMQ down"))
        .when(rabbitTemplate)
        .convertAndSend(
            eq(SCAN_EXCHANGE),
            eq(SCAN_NOTIFICATION_RK),
            any(ScanNotificationEvent.class),
            any(MessagePostProcessor.class)
        );

    // Не должно выбрасывать исключение наружу
    scanNotificationProducer.sendScanCompletedNotification(result);

    verify(rabbitTemplate).convertAndSend(
        eq(SCAN_EXCHANGE),
        eq(SCAN_NOTIFICATION_RK),
        any(ScanNotificationEvent.class),
        any(MessagePostProcessor.class)
    );
  }

  @Test
  void shouldSurviveSendResultUpdatedException() {
    ScanResult result = ScanResult.processing(taskId, 1L);

    doThrow(new RuntimeException("Connection lost"))
        .when(rabbitTemplate)
        .convertAndSend(
            eq(SCAN_EXCHANGE),
            eq(SCAN_NOTIFICATION_RK),
            any(ScanNotificationEvent.class),
            any(MessagePostProcessor.class)
        );

    scanNotificationProducer.sendResultUpdatedNotification(result);
  }
}