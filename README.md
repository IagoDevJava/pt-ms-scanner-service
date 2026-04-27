# 📷 Scanner Service - Микросервис-оркестратор сканирования продуктов

[![Java Version](https://img.shields.io/badge/Java-21-blue.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.6-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3.12-orange.svg)](https://www.rabbitmq.com/)
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

**Scanner Service** — лёгкий микросервис-оркестратор, который предоставляет REST API для идентификации продуктов по штрих-коду или фотографии.  
Он не выполняет распознавание самостоятельно, а координирует взаимодействие между клиентами, базой продуктов и специализированным сервисом комплексного сканирования (`ms-complex-scan`) через RabbitMQ.

### Ключевые особенности:
- ⚡ **Быстрый поиск по штрих-коду** с двухуровневым кэшированием (in-memory Caffeine + внешний сервис-библиотека)
- 🔄 **Асинхронное сканирование изображений** — запросы передаются в очередь и обрабатываются независимым worker-сервисом
- ✅ **Отслеживание статуса задач** — клиент может запросить прогресс и получить результат, когда он будет готов
- 💬 **Уведомления о завершении** — результаты сканирования доставляются клиентам через выделенную очередь RabbitMQ
- 📊 **Production-ready метрики** (Prometheus + Grafana)
- 🛡️ **Отказоустойчивость** — повторные попытки на уровне брокера, Dead Letter Queue для проблемных сообщений

## 🏗 Архитектура

```mermaid
graph LR
    Client[Клиент (ms-storage-service)] -->|REST: /scan/barcode| Scanner[ms-scanner-service]
    Client -->|REST: /scan/complex| Scanner
    Client -->|REST: /scan/status/{id}| Scanner
    
    Scanner -->|Feign| Library[ms-food-library]
    Scanner -->|Кэш Caffeine| Cache[(In-Memory Cache)]
    
    Scanner -->|Публикация запроса| Exchange[scan.exchange]
    Exchange -->|routing: scan.request| QueueReq[scan.requests.queue]
    QueueReq --> ComplexScan[ms-complex-scan]
    
    ComplexScan -->|routing: scan.result| Exchange
    Exchange -->|routing: scan.result| QueueRes[scan.results.queue]
    QueueRes --> Scanner
    
    Scanner -->|routing: scan.notification| Exchange
    Exchange -->|routing: scan.notification| QueueNotif[scan.notifications.queue]
    QueueNotif --> Client
    
    style Client fill:#e8f5e9
    style Scanner fill:#e3f2fd,stroke:#1565c0,stroke-width:2px
    style ComplexScan fill:#f3e5f5
    style Library fill:#fff9c4