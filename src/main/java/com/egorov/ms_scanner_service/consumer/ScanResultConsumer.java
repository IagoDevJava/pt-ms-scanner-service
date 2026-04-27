package com.egorov.ms_scanner_service.consumer;

import static com.egorov.ms_scanner_service.config.RabbitMQConfig.SCAN_RESULTS_QUEUE;

import com.egorov.ms_scanner_service.model.ScanResult;
import com.egorov.ms_scanner_service.producer.ScanNotificationProducer;
import com.egorov.ms_scanner_service.service.ScanServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScanResultConsumer {

  private final ScanServiceImpl scanService;
  private final ScanNotificationProducer notificationProducer;

  @RabbitListener(queues = SCAN_RESULTS_QUEUE, concurrency = "5-20")
  public void consumeScanResult(ScanResult scanResult) {
    log.info("Received scan result: taskId={}, status={}",
        scanResult.taskId(), scanResult.status());

    scanService.updateResult(scanResult);

    notifyClient(scanResult);

    log.info("Processed scan result: taskId={}, status={}, time={}ms",
        scanResult.taskId(), scanResult.status(), scanResult.processingTimeMs());
  }

  private void notifyClient(ScanResult result) {
    switch (result.status()) {
      case COMPLETED:
        notificationProducer.sendScanCompletedNotification(result);
        break;

      case FAILED:
        notificationProducer.sendScanCompletedNotification(result);
        log.warn("Scan failed for taskId={}, message={}",
            result.taskId(), result.message());
        break;

      case PROCESSING:
        notificationProducer.sendResultUpdatedNotification(result);
        break;

      default:
        log.debug("No notification needed for status: {}", result.status());
        break;
    }
  }
}