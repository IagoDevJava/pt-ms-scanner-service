.PHONY: help up down restart logs clean test build run

# Цвета для вывода
YELLOW := \033[1;33m
GREEN  := \033[1;32m
NC     := \033[0m

help: ## Показать это сообщение
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "$(YELLOW)%-20s$(NC) %s\n", $$1, $$2}'

# =========== Docker / Infrastructure ===========

up: ## Запустить инфраструктуру (RabbitMQ)
	@echo "$(GREEN)Starting infrastructure...$(NC)"
	docker-compose up -d rabbitmq
	@echo "$(GREEN)Waiting for RabbitMQ to be healthy...$(NC)"
	@until docker inspect -f '{{.State.Health.Status}}' scanner-rabbitmq | grep -q healthy; do sleep 2; done
	@echo "$(GREEN)RabbitMQ is ready!$(NC)"
	@echo "$(GREEN)Management UI: http://localhost:15672 (guest/guest)$(NC)"

down: ## Остановить инфраструктуру
	@echo "$(YELLOW)Stopping infrastructure...$(NC)"
	docker-compose down

restart: down up ## Перезапустить инфраструктуру

logs: ## Показать логи RabbitMQ
	docker-compose logs -f rabbitmq

clean: down ## Остановить и удалить все данные
	@echo "$(YELLOW)Removing volumes...$(NC)"
	docker-compose down -v
	rm -rf logs/

# =========== Application ===========

build: ## Собрать проект
	@echo "$(GREEN)Building project...$(NC)"
	mvn clean package -DskipTests

test: ## Запустить все тесты
	@echo "$(GREEN)Running tests...$(NC)"
	mvn test

test-integration: up ## Запустить интеграционные тесты
	@echo "$(GREEN)Running integration tests...$(NC)"
	mvn verify -Pintegration
	@$(MAKE) down

run: up ## Запустить приложение локально (dev профиль)
	@echo "$(GREEN)Starting ms-scanner-service...$(NC)"
	mvn spring-boot:run -Dspring-boot.run.profiles=dev

run-jar: build up ## Собрать и запустить JAR
	@echo "$(GREEN)Starting from JAR...$(NC)"
	java -jar target/ms-scanner-service-*.jar --spring.profiles.active=dev

# =========== Docker Image ===========

docker-build: ## Собрать Docker образ
	@echo "$(GREEN)Building Docker image...$(NC)"
	mvn spring-boot:build-image

docker-run: docker-build up ## Собрать образ и запустить контейнер
	@echo "$(GREEN)Starting Docker container...$(NC)"
	docker run -d \
		--name ms-scanner-service \
		--network scanner-scanner-network \
		-p 8083:8083 \
		-e RABBITMQ_HOST=scanner-rabbitmq \
		-e RABBITMQ_PORT=5672 \
		-e SPRING_PROFILES_ACTIVE=dev \
		food-scanner/ms-scanner-service:0.0.1-SNAPSHOT

# =========== Utility ===========

check-rabbitmq: ## Проверить статус RabbitMQ
	@curl -s -u guest:guest http://localhost:15672/api/aliveness-test/%2F | python -m json.tool

check-app: ## Проверить health приложения
	@curl -s http://localhost:8083/actuator/health | python -m json.tool