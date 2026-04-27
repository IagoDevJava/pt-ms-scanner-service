package com.egorov.ms_scanner_service.producer;

import com.egorov.ms_scanner_service.config.RabbitMQConfig;
import com.egorov.ms_scanner_service.model.ScanNotificationEvent;
import com.egorov.ms_scanner_service.model.ScanResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScanNotificationProducer {

  private final RabbitTemplate rabbitTemplate;

  /**
   * Отправка уведомления о завершении сканирования
   */
  public void sendScanCompletedNotification(ScanResult result) {
    ScanNotificationEvent event = ScanNotificationEvent.from(
        result,
        result.status() == com.egorov.ms_scanner_service.model.ScanStatus.COMPLETED
            ? ScanNotificationEvent.NotificationType.SCAN_COMPLETED
            : ScanNotificationEvent.NotificationType.SCAN_FAILED
    );

    sendNotification(event);
  }

  /**
   * Отправка уведомления об обновлении результата
   */
  public void sendResultUpdatedNotification(ScanResult result) {
    ScanNotificationEvent event = ScanNotificationEvent.from(
        result,
        ScanNotificationEvent.NotificationType.RESULT_UPDATED
    );

    sendNotification(event);
  }

  private void sendNotification(ScanNotificationEvent event) {
    log.info("Sending scan notification: taskId={}, type={}, status={}",
        event.taskId(), event.notificationType(), event.status());

    try {
      rabbitTemplate.convertAndSend(
          RabbitMQConfig.SCAN_EXCHANGE,
          RabbitMQConfig.SCAN_NOTIFICATION_RK,
          event,
          message -> {
            // Устанавливаем приоритет для важных уведомлений
            message.getMessageProperties().setPriority(
                event.status() == com.egorov.ms_scanner_service.model.ScanStatus.COMPLETED ? 5 : 3
            );
            // Добавляем expiration для устаревших уведомлений
            message.getMessageProperties().setExpiration("60000"); // 1 минута
            return message;
          }
      );

      log.debug("Scan notification sent successfully: taskId={}", event.taskId());
    } catch (Exception e) {
      log.error("Failed to send scan notification: taskId={}, error={}",
          event.taskId(), e.getMessage(), e);
      // Не пробрасываем исключение, чтобы не блокировать обработку результата
      // В production здесь можно добавить fallback логику
    }
  }
}