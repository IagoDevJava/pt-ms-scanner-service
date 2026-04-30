# 📷 Scanner Service - Микросервис сканирования продуктов

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
- [Roadmap](#-roadmap)

## 🎯 Обзор

**Scanner Service** — лёгкий микросервис-оркестратор, который предоставляет REST API для
идентификации продуктов по штрих-коду или фотографии.  
Он не выполняет распознавание самостоятельно, а координирует взаимодействие между клиентами, базой
продуктов и специализированным сервисом комплексного сканирования (`ms-complex-scan`) через
RabbitMQ.

**Миграция на MinIO**  
Все изображения, полученные через API `/scan/complex`, временно сохраняются в объектном хранилище [MinIO](https://min.io/) (S3-совместимое). В очередь `scan.requests.queue` передаётся только URL изображения, а не base64, что позволяет избежать перегрузки RabbitMQ и гарантирует масштабируемость.

### Ключевые особенности:

- ⚡ **Быстрый поиск по штрих-коду** с двухуровневым кэшированием (in-memory Caffeine + внешний
  сервис-библиотека)
- 🔄 **Асинхронное сканирование изображений** — запросы передаются в очередь и обрабатываются
  независимым worker-сервисом
- ✅ **Отслеживание статуса задач** — клиент может запросить прогресс и получить результат, когда он
  будет готов
- 💬 **Уведомления о завершении** — результаты сканирования доставляются клиентам через выделенную
  очередь RabbitMQ
- 🛡️ **Отказоустойчивость** — CircuitBreaker для Feign-клиента, кастомные исключения, повторные
  попытки на уровне брокера, Dead Letter Queue
- 📊 **Production-ready метрики** — гистограммы времени обработки, статистика кэша

## 🏗 Архитектура

graph LR

    Client[Клиент (ms-storage-service)] -->|REST: /scan/barcode| Scanner[ms-scanner-service]
    Client -->|REST: /scan/complex| Scanner
    Client -->|REST: /scan/status/{id}| Scanner
    
    Scanner -->|Feign| Library[ms-food-library]
    Scanner -->|Кэш Caffeine| Cache[(In-Memory Cache)]
    MinIO -->|imageUrl| Scanner
    
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

**Быстрый поиск:**

Клиент отправляет штрих-код → `ms-scanner-service` проверяет кэш Caffeine → если промах, вызывает
`ms-food-library` → возвращает результат.

**Сложное сканирование:**

Клиент отправляет изображение → сервис сохраняет задачу со статусом `PENDING` → отправляет запрос в
`scan.requests.queue` → `ms-complex-scan` выполняет OCR/ML, сохраняет продукт в библиотеку и
возвращает результат в `scan.results.queue` → `ms-scanner-service` обновляет статус и отправляет
уведомление в `scan.notifications.queue` → клиент получает результат.

## 🚀 Основные возможности

- **GET /api/v1/scan/status/{taskId}** – получение статуса асинхронной задачи
- **POST /api/v1/scan/barcode** – поиск продукта по штрих-коду
- **POST /api/v1/scan/complex** – запуск асинхронного распознавания по изображению
- Автоматическое кэширование найденных продуктов (Caffeine)
- Интеграция с MinIO для хранения исходных изображений
- Публикация уведомлений о завершении сканирования
- Health-чеки готовности (readiness, liveness) с проверкой RabbitMQ и Feign-клиента

## 💻 Технологический стек

| Категория          | Технология                           |
|--------------------|--------------------------------------|
| Язык               | Java 21                              |
| Фреймворк          | Spring Boot 4.0.6                    |
| Коммуникация       | REST API (Spring Web), Feign Client  |
| Брокер сообщений   | RabbitMQ (Spring AMQP)               |
| Объектное хранилище| MinIO (S3 API)                       |
| Отказоустойчивость | Resilience4j (CircuitBreaker)        |
| Кэширование        | Caffeine (in‑memory)                 |
| Мониторинг         | Actuator + Prometheus endpoint       |
| Сборка             | Maven                                |
| Контейнеризация    | Docker, Testcontainers               |
| Тестирование       | JUnit 5, Mockito, Spring Rabbit Test |

## 🔧 Начало работы

### Предварительные требования
- JDK 21+
- Docker и Docker Compose
- Maven 3.9+
- Make (опционально, для удобства)

### Быстрый старт (Make)

```bash
# Клонировать репозиторий
git clone https://github.com/your-org/ms-scanner-service.git
cd ms-scanner-service
```
### Быстрый старт (без Make)

#### Запустить RabbitMQ
```docker-compose up -d rabbitmq```

#### Подождать готовности RabbitMQ (опционально)
```docker inspect -f "{{.State.Health.Status}}" scanner-rabbitmq```

#### Запустить приложение
```mvn spring-boot:run -Dspring-boot.run.profiles=dev```

Откроется:
- Приложение: http://localhost:8083
- RabbitMQ Management: http://localhost:15672 (guest/guest)
- Health check: http://localhost:8083/actuator/health


### Ручной запуск (если RabbitMQ уже где-то запущен)
С указанием хоста RabbitMQ
```RABBITMQ_HOST=my-rabbitmq-host mvn spring-boot:run -Dspring-boot.run.profiles=dev```

## 📚 API Документация

1. **Быстрое сканирование по штрих-коду**
   #### POST /api/v1/scan/barcode
   Тело запроса:
   ```json
   {
    "userId": 123,
    "barcode": "4601234567890"
    }
   ```
   #### Успешный ответ (200 OK):
    ```json
   {
    "found": true,
    "product": {
    "id": 1,
    "name": "Молоко 3.2%",
    "barcode": "4601234567890",
    "calories": 60.0
    },
    "message": "Продукт найден в базе",
    "processingTimeMs": 12
    }
   ```
   #### Продукт не найден (404 NOT FOUND):
    ```json
   {
    "found": false,
    "product": null,
    "message": "Продукт не найден. Сфотографируйте упаковку или добавьте вручную.",
    "barcode": "4601234567890",
    "processingTimeMs": 25
    }
   ```
2. **Сложное сканирование по изображению**
   #### POST /api/v1/scan/complex
   Тело запроса:
   ```json
   {
    "taskId": "550e8400-e29b-41d4-a716-446655440000",
    "userId": 123,
    "imageBase64": "iVBORw0KGgoAAAANSUhEUgAA...",
    "scanType": "AUTO"
    }
   ```
   #### Успешный ответ (200 OK):
    ```json
   {
    "taskId": "550e8400-e29b-41d4-a716-446655440000",
    "userId": 123,
    "status": "PENDING",
    "product": null,
    "message": "Ожидайте, идёт обработка."
    }
   ```
3. **Проверка статуса задачи**
   #### GET /api/v1/scan/status/{taskId}
   Ответ (в процессе):
   ```json
   {
    "taskId": "550e8400-e29b-41d4-a716-446655440000",
    "status": "PROCESSING",
    "message": "Фото отправлено на обработку. Мы уведомим вас о результате."
    }
   ```
   Ответ (завершено):
   ```json
   {
    "taskId": "550e8400-e29b-41d4-a716-446655440000",
    "status": "COMPLETED",
    "product": { 
     
    },
    "processingTimeMs": 3200
    }
   ```

## ⚙️ Конфигурация

Основные параметры задаются через `application.yml` и переменные окружения.

| Параметр                        | Описание                                | Значение по умолчанию |
|---------------------------------|-----------------------------------------|-----------------------|
| `RABBITMQ_HOST`                 | Хост RabbitMQ                           | localhost             |
| `LIBRARY_SERVICE_URL`           | URL сервиса продуктов (не используется) | http://localhost:8081 |
| `barcode.cache.max-size`        | Максимальный размер кэша штрих-кодов    | 100000                |
| `barcode.cache.ttl-hours`       | Время жизни записей кэша (часы)         | 24                    |
| `scan-result.cache.max-size`    | Размер кэша статусов задач              | 10000                 |
| `scan-result.cache.ttl-minutes` | Время жизни статуса задачи (минуты)     | 10                    |
| `resilience4j.circuitbreaker.*` | Настройки CircuitBreaker для Feign      | См. application.yml   |
| `resilience4j.timelimiter.*`    | Таймауты для Feign-клиента              | См. application.yml   |

## 📊 Мониторинг

Метрики доступны через Actuator по `/actuator/metrics` и в формате Prometheus по
`/actuator/prometheus`.

Ключевые health-индикаторы:

- `readinessState` — готовность принимать трафик
- `livenessState` — живость приложения
- `rabbitmq` — доступность брокера
- `diskSpace` — доступное место на диске

## 🛡️ Обработка ошибок

Сервис использует кастомную иерархию исключений для точной диагностики проблем:

| Исключение              | Ситуация                               | HTTP-статус | Поведение                           |
|-------------------------|----------------------------------------|-------------|-------------------------------------|
| `BarcodeScanException`  | Ошибка поиска по штрих-коду            | 404         | Возвращается `not found`            |
| `FoodLibraryException`  | Недоступен сервис библиотеки продуктов | 404         | Срабатывает CircuitBreaker fallback |
| `QueuePublishException` | Ошибка отправки в RabbitMQ             | 200         | Возвращается `ScanResult.failed`    |
| `CacheException`        | Ошибка кэширования                     | 200         | Деградация: работа без кэша         |

Все исключения логируются с уровнем ERROR и полным стектрейсом.

## 🧪 Тестирование

### Общая стратегия

Тесты разделены на три уровня:

- **Юнит-тесты** — изолированная проверка сервисов и оркестратора (`ScanServiceImpl`,
  `CacheService`)
- **Интеграционные тесты контроллеров** — проверка REST API с замоканными внешними зависимостями (
  Feign, RabbitMQ)
- **Интеграционные тесты с брокером** — проверка консьюмеров с замоканным RabbitTemplate

### Профиль `test`

В тестах используется профиль `test` со следующими особенностями:

- Случайный порт сервера (`server.port=0`)
- Отключены слушатели RabbitMQ (`auto-startup: false`)
- Замокан Feign-клиент (`@MockitoBean`)
- Кэши с минимальными значениями (100 записей, TTL 1 минута/час)
- Отключены Prometheus и трейсинг
- Минимальное логирование (WARN для фреймворков, DEBUG для кода сервиса)

### Запуск тестов

**Все тесты:**

```bash
mvn test
```

**Только юнит-тесты:**

```bash
  mvn test -Dtest="*Test" -DfailIfNoTests=false
````

**С определённым профилем:**

```bash
  mvn test -Dspring.profiles.active=test
```

## Покрытие тестами

| Класс                       | Тип тестов              | Что проверяется                                                                |
|-----------------------------|-------------------------|--------------------------------------------------------------------------------|
| `ScanControllerBarcodeTest` | Интеграционный (Spring) | Поиск по штрих-коду: попадание в кэш, вызов Feign, not found, обработка ошибок |
| `ScanControllerComplexTest` | Интеграционный (Spring) | Запуск сложного сканирования, проверка статуса, 404 для несуществующих задач   |
| `ScanResultConsumerTest`    | Интеграционный (Spring) | Получение результата из очереди, обновление кэша, отправка уведомлений         |
| `ScanServiceImplTest`       | Юнит (Mockito)          | Оркестрация: flow сканирования, обработка исключений, граничные случаи         |
| `CacheServiceTest`          | Юнит (без Spring)       | Кэширование продуктов и результатов, TTL, статистика                           |

## Примеры тестовых сценариев

**Проверка кэширования:**

```
@Test
void shouldReturnProductFromCacheOnSecondRequest() throws Exception {
   // Первый запрос — продукт найден через Feign и сохранён в кэш
   // Второй запрос — продукт взят из кэша, Feign не вызывается
}
```

**Проверка отказоустойчивости:**

```
@Test
void shouldReturnNotFoundWhenLibraryServiceThrowsException() throws Exception {
    // Feign-клиент бросает исключение → возвращается not found вместо 500
}
```

**Проверка кастомных исключений:**

```
@Test
void shouldReturnFailedWhenQueuePublishException() {
    // Ошибка отправки в RabbitMQ → возвращается ScanResult.failed
}
```

## 🚢 Деплой

Docker-образ можно собрать с помощью Spring Boot Maven Plugin:

```bash
    mvn spring-boot:build-image
```

Рекомендуется запускать с профилем prod и обязательным указанием переменных окружения для
подключения
к RabbitMQ и Library Service.

## 🗺 Roadmap

- ✅ Двухуровневое кэширование (Caffeine + Feign)
- ✅ Асинхронное сканирование через RabbitMQ
- ✅ Уведомления о завершении задачи
- ✅ CircuitBreaker для отказоустойчивости
- ⏳ Хранение истории запросов в PostgreSQL
- ⏳ Поддержка WebSocket для real-time уведомлений
- ⏳ Rate limiting на API
- ⏳ Кэш с распределённой инвалидацией (Redis)
- ⏳ Повторные попытки (Retry) для Feign-клиента
