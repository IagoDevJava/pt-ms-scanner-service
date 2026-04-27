package com.egorov.ms_scanner_service.model;

public enum ScanType {
  BARCODE,
  OCR,        // Распознавание текста
  ML,         // ML анализ
  AUTO        // Автоматический проход по всем методам
}
