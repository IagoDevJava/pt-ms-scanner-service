# 📷 Scanner Service - Микросервис распознавания продуктов

[![Java Version](https://img.shields.io/badge/Java-21-blue.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3.12-orange.svg)](https://www.rabbitmq.com/)
[![TensorFlow Lite](https://img.shields.io/badge/TensorFlow%20Lite-2.15-ff6f00.svg)](https://www.tensorflow.org/lite)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

## 📋 Оглавление
- [Обзор](#-обзор)
- [Архитектура](#-архитектура)
- [Основные возможности](#-основные-возможности)
- [Технологический стек](#-технологический-стек)
- [Начало работы](#-начало-работы)
- [API Документация](#-api-документация)
- [Конфигурация](#-конфигурация)
- [Мониторинг](#-мониторинг)
- [Деплой](#-деплой)
- [Тестирование](#-тестирование)
- [Производительность](#-производительность)
- [Roadmap](#-roadmap)

## 🎯 Обзор

**Scanner Service** - высокопроизводительный микросервис для интеллектуального распознавания продуктов питания с использованием компьютерного зрения и машинного обучения. Сервис поддерживает 3 метода распознавания и может обрабатывать до 1000 запросов в секунду.

### Ключевые особенности:
- 🚀 **Виртуальные потоки Java 21** для максимальной производительности
- 🔄 **Асинхронная обработка** через RabbitMQ
- 🧠 **ML классификация** продуктов (TensorFlow Lite)
- 📸 **OCR распознавание** текста на упаковках
- 📊 **Production-ready метрики** (Prometheus + Grafana)
- 🛡️ **Отказоустойчивость** (Retry, DLQ, Circuit Breaker)

## 🏗 Архитектура

```mermaid
graph TB
    Client[Мобильное приложение] --> API[API Gateway]
    API --> |Синхронно| Sync[Sync Scan]
    API --> |Асинхронно| Queue[RabbitMQ]
    
    Queue --> |scan.requests| Consumer[RabbitMQ Consumer]
    Consumer --> Orchestrator[Scan Orchestrator]
    
    Orchestrator --> Barcode[Barcode Scanner<br/>ZXing]
    Orchestrator --> OCR[OCR Scanner<br/>Tesseract]
    Orchestrator --> ML[ML Classifier<br/>TensorFlow]
    
    Barcode --> Library[Library Service]
    OCR --> Library
    ML --> Library
    
    Orchestrator --> |Результат| Queue
    Queue --> |scan.results| Storage[Storage Service]
    
    style Client fill:#e1f5fe
    style Queue fill:#fff3e0
    style ML fill:#f3e5f5
