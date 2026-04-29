package com.egorov.ms_scanner_service.controller;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.egorov.ms_scanner_service.feign.wrapper.FoodLibraryClientWrapper;
import com.egorov.ms_scanner_service.model.ProductInfo;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
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
class ScanControllerBarcodeTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private FoodLibraryClientWrapper foodLibraryClient;

  @MockitoBean
  private RabbitTemplate rabbitTemplate;

  private final ProductInfo sampleProduct = new ProductInfo(
      1L, 2L, "Молоко", "Свежее молоко 3.2%", "4601234567890",
      60.0, 3.0, 3.2, 4.7, 30, false, "Хранить при t° от +2°C до +6°C",
      BigDecimal.valueOf(2), BigDecimal.valueOf(6), 75, 14, false,
      "ext-001", null, null
  );

  @Test
  void shouldReturnProductFromCacheOnSecondRequest() throws Exception {
    String barcode = "4601234567890";
    when(foodLibraryClient.findByBarcode(barcode)).thenReturn(Optional.of(sampleProduct));

    mockMvc.perform(post("/api/v1/scan/barcode")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"userId\": 123, \"barcode\": \"" + barcode + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.found").value(true))
        .andExpect(jsonPath("$.product.name").value("Молоко"));

    verify(foodLibraryClient, times(1)).findByBarcode(barcode);

    mockMvc.perform(post("/api/v1/scan/barcode")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"userId\": 123, \"barcode\": \"" + barcode + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.found").value(true));

    verify(foodLibraryClient, times(1)).findByBarcode(barcode);
  }

  @Test
  void shouldReturnNotFoundWhenProductNotInCacheNorLibrary() throws Exception {
    String barcode = "9999999999999";
    when(foodLibraryClient.findByBarcode(barcode)).thenReturn(Optional.empty());

    mockMvc.perform(post("/api/v1/scan/barcode")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"userId\": 321, \"barcode\": \"" + barcode + "\"}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.found").value(false))
        .andExpect(jsonPath("$.barcode").value(barcode))
        .andExpect(jsonPath("$.message").isNotEmpty());
  }

  @Test
  void shouldReturnNotFoundWhenLibraryServiceThrowsException() throws Exception {
    String barcode = "4600000000000";
    when(foodLibraryClient.findByBarcode(barcode)).thenReturn(Optional.empty());

    mockMvc.perform(post("/api/v1/scan/barcode")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"userId\": 111, \"barcode\": \"" + barcode + "\"}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.found").value(false));
  }
}