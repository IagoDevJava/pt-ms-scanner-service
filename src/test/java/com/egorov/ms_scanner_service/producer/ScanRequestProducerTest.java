package com.egorov.ms_scanner_service.producer;

import static com.egorov.ms_scanner_service.config.RabbitMQConfig.SCAN_EXCHANGE;
import static com.egorov.ms_scanner_service.config.RabbitMQConfig.SCAN_REQUEST_RK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.egorov.ms_scanner_service.exception.QueuePublishException;
import com.egorov.ms_scanner_service.mapper.ComplexScanRequestMapper;
import com.egorov.ms_scanner_service.model.ExternalComplexScanRequest;
import com.egorov.ms_scanner_service.model.InternalComplexScanRequest;
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
    String imageUrl = "http://minio/test.jpg";
    ExternalComplexScanRequest externalRequest = new ExternalComplexScanRequest(
        taskId, 1L, "base64image", ScanType.AUTO, null
    );
    InternalComplexScanRequest internalRequest = ComplexScanRequestMapper.toInternalRequest(
        externalRequest, imageUrl);

    scanRequestProducer.sendRequest(internalRequest);

    verify(rabbitTemplate).convertAndSend(
        eq(SCAN_EXCHANGE),
        eq(SCAN_REQUEST_RK),
        eq(internalRequest)
    );
  }

  @Test
  void shouldSendRequestWithCorrectExchangeAndRoutingKey() {
    UUID taskId = UUID.randomUUID();
    String imageUrl = "http://minio/test.jpg";
    ExternalComplexScanRequest externalRequest = new ExternalComplexScanRequest(
        taskId, 2L, "imageData", ScanType.ML, null
    );
    InternalComplexScanRequest internalRequest = ComplexScanRequestMapper.toInternalRequest(
        externalRequest, imageUrl);

    scanRequestProducer.sendRequest(internalRequest);

    ArgumentCaptor<String> exchangeCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> routingKeyCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<InternalComplexScanRequest> requestCaptor = ArgumentCaptor.forClass(
        InternalComplexScanRequest.class);

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
    String imageUrl = "http://minio/test.jpg";
    ExternalComplexScanRequest externalRequest = new ExternalComplexScanRequest(
        taskId, 1L, "base64image", ScanType.AUTO, null
    );
    InternalComplexScanRequest internalRequest = ComplexScanRequestMapper.toInternalRequest(
        externalRequest, imageUrl);

    RuntimeException rabbitException = new RuntimeException("Connection refused");
    doThrow(rabbitException)
        .when(rabbitTemplate)
        .convertAndSend(SCAN_EXCHANGE, SCAN_REQUEST_RK, internalRequest);

    assertThatThrownBy(() -> scanRequestProducer.sendRequest(internalRequest))
        .isInstanceOf(QueuePublishException.class)
        .hasMessageContaining("Failed to send scan request to queue")
        .hasCause(rabbitException)
        .extracting(ex -> ((QueuePublishException) ex).getTaskId())
        .isEqualTo(taskId);
  }

  @Test
  void shouldPropagateQueuePublishExceptionWithCorrectTaskId() {
    UUID taskId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    String imageUrl = "http://minio/test.jpg";
    ExternalComplexScanRequest externalRequest = new ExternalComplexScanRequest(
        taskId, 99L, "test", ScanType.OCR, null
    );
    InternalComplexScanRequest internalRequest = ComplexScanRequestMapper.toInternalRequest(
        externalRequest, imageUrl);

    doThrow(new RuntimeException("Broken pipe"))
        .when(rabbitTemplate)
        .convertAndSend(SCAN_EXCHANGE, SCAN_REQUEST_RK, internalRequest);

    assertThatThrownBy(() -> scanRequestProducer.sendRequest(internalRequest))
        .isInstanceOf(QueuePublishException.class)
        .matches(ex -> ((QueuePublishException) ex).getTaskId().equals(taskId))
        .matches(ex -> ex.getMessage().contains("Failed to send scan request to queue"));
  }
}