package com.egorov.ms_scanner_service.model;

import lombok.Getter;

@Getter
public enum ScanStatus {
  PENDING("В ожидании"),
  PROCESSING("Обработка"),
  COMPLETED("Завершено"),
  FAILED("Ошибка"),
  RETRY("Повторная попытка");

  private final String description;

  ScanStatus(String description) {
    this.description = description;
  }

}
