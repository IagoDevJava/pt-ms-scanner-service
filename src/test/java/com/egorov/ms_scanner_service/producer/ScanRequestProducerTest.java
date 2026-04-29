package com.egorov.ms_scanner_service.producer;

import static com.egorov.ms_scanner_service.config.RabbitMQConfig.SCAN_EXCHANGE;
import static com.egorov.ms_scanner_service.config.RabbitMQConfig.SCAN_REQUEST_RK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.egorov.ms_scanner_service.exception.QueuePublishException;
import com.egorov.ms_scanner_service.model.ComplexScanRequest;
import com.egorov.ms_scanner_service.model.ScanType;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@ExtendWith(MockitoExtension.class)
class ScanRequestProducerTest {

  @Mock
  private RabbitTemplate rabbitTemplate;

  @InjectMocks
  private ScanRequestProducer scanRequestProducer;

  @Test
  void shouldSendRequestSuccessfully() {
    UUID taskId = UUID.randomUUID();
    ComplexScanRequest request = new ComplexScanRequest(
        taskId, 1L, "base64image", ScanType.AUTO, null
    );

    scanRequestProducer.sendRequest(request);

    verify(rabbitTemplate).convertAndSend(
        eq(SCAN_EXCHANGE),
        eq(SCAN_REQUEST_RK),
        eq(request)
    );
  }

  @Test
  void shouldSendRequestWithCorrectExchangeAndRoutingKey() {
    UUID taskId = UUID.randomUUID();
    ComplexScanRequest request = new ComplexScanRequest(
        taskId, 2L, "imageData", ScanType.ML, null
    );

    scanRequestProducer.sendRequest(request);

    ArgumentCaptor<String> exchangeCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> routingKeyCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<ComplexScanRequest> requestCaptor = ArgumentCaptor.forClass(
        ComplexScanRequest.class);

    verify(rabbitTemplate).convertAndSend(
        exchangeCaptor.capture(),
        routingKeyCaptor.capture(),
        requestCaptor.capture()
    );

    assertThat(exchangeCaptor.getValue()).isEqualTo(SCAN_EXCHANGE);
    assertThat(routingKeyCaptor.getValue()).isEqualTo(SCAN_REQUEST_RK);
    assertThat(requestCaptor.getValue().taskId()).isEqualTo(taskId);
    assertThat(requestCaptor.getValue().userId()).isEqualTo(2L);
    assertThat(requestCaptor.getValue().scanType()).isEqualTo(ScanType.ML);
  }

  @Test
  void shouldThrowQueuePublishExceptionWhenRabbitFails() {
    UUID taskId = UUID.randomUUID();
    ComplexScanRequest request = new ComplexScanRequest(
        taskId, 1L, "base64image", ScanType.AUTO, null
    );

    RuntimeException rabbitException = new RuntimeException("Connection refused");
    doThrow(rabbitException)
        .when(rabbitTemplate)
        .convertAndSend(eq(SCAN_EXCHANGE), eq(SCAN_REQUEST_RK), eq(request));

    assertThatThrownBy(() -> scanRequestProducer.sendRequest(request))
        .isInstanceOf(QueuePublishException.class)
        .hasMessageContaining("Failed to send scan request to queue")
        .hasCause(rabbitException)
        .extracting(ex -> ((QueuePublishException) ex).getTaskId())
        .isEqualTo(taskId);
  }

  @Test
  void shouldPropagateQueuePublishExceptionWithCorrectTaskId() {
    UUID taskId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    ComplexScanRequest request = new ComplexScanRequest(
        taskId, 99L, "test", ScanType.OCR, null
    );

    doThrow(new RuntimeException("Broken pipe"))
        .when(rabbitTemplate)
        .convertAndSend(eq(SCAN_EXCHANGE), eq(SCAN_REQUEST_RK), eq(request));

    assertThatThrownBy(() -> scanRequestProducer.sendRequest(request))
        .isInstanceOf(QueuePublishException.class)
        .matches(ex -> ((QueuePublishException) ex).getTaskId().equals(taskId))
        .matches(ex -> ex.getMessage().contains("Failed to send scan request to queue"));
  }
}