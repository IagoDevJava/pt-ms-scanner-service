package com.egorov.ms_scanner_service.producer;

import static com.egorov.ms_scanner_service.config.RabbitMQConfig.SCAN_EXCHANGE;
import static com.egorov.ms_scanner_service.config.RabbitMQConfig.SCAN_REQUEST_RK;

import com.egorov.ms_scanner_service.exception.QueuePublishException;
import com.egorov.ms_scanner_service.model.ComplexScanRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScanRequestProducer {

  private final RabbitTemplate rabbitTemplate;

  public void sendRequest(ComplexScanRequest request) {
    log.info("Sending scan request: taskId={}, userId={}, type={}",
        request.taskId(), request.userId(), request.scanType());

    try {
      rabbitTemplate.convertAndSend(SCAN_EXCHANGE, SCAN_REQUEST_RK, request);
      log.debug("Scan request sent successfully: taskId={}", request.taskId());
    } catch (Exception e) {
      log.error("Failed to send scan request: taskId={}", request.taskId(), e);
      throw new QueuePublishException(
          request.taskId(),
          "Failed to send scan request to queue",
          e
      );
    }
  }
}