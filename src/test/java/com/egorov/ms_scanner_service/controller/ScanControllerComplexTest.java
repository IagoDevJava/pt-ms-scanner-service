package com.egorov.ms_scanner_service.controller;

import static com.egorov.ms_scanner_service.config.RabbitMQConfig.SCAN_EXCHANGE;
import static com.egorov.ms_scanner_service.config.RabbitMQConfig.SCAN_REQUEST_RK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.egorov.ms_scanner_service.feign.FoodLibraryClient;
import com.egorov.ms_scanner_service.model.ScanResult;
import com.egorov.ms_scanner_service.model.ScanStatus;
import com.egorov.ms_scanner_service.service.CacheService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ScanControllerComplexTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private CacheService cacheService;

  @MockitoBean
  private RabbitTemplate rabbitTemplate;

  @MockitoBean
  private FoodLibraryClient foodLibraryClient;

  @Test
  void shouldReturnPendingResultAndSendMessageToQueue() throws Exception {
    UUID taskId = UUID.randomUUID();
    String requestJson = String.format("""
        {
            "taskId": "%s",
            "userId": 1,
            "imageBase64": "base64string"
        }
        """, taskId);

    mockMvc.perform(post("/api/v1/scan/complex")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestJson))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andExpect(jsonPath("$.taskId").value(taskId.toString()));

    ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
    verify(rabbitTemplate).convertAndSend(eq(SCAN_EXCHANGE), eq(SCAN_REQUEST_RK), captor.capture());

    ScanResult cached = cacheService.getTaskStatus(taskId);
    assertThat(cached).isNotNull();
    assertThat(cached.status()).isEqualTo(ScanStatus.PENDING);
  }

  @Test
  void shouldReturnTaskStatusWhenExists() throws Exception {
    UUID taskId = UUID.randomUUID();
    ScanResult result = ScanResult.processing(taskId, 1L);
    cacheService.putScanResult(taskId, result);

    mockMvc.perform(get("/api/v1/scan/status/" + taskId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PROCESSING"))
        .andExpect(jsonPath("$.taskId").value(taskId.toString()));
  }

  @Test
  void shouldReturn404WhenTaskNotFound() throws Exception {
    mockMvc.perform(get("/api/v1/scan/status/" + UUID.randomUUID()))
        .andExpect(status().isNotFound());
  }
}