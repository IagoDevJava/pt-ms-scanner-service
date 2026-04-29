#!/bin/bash
set -e

echo "=========================================="
echo "  Starting MS-Scanner-Service Dev Env"
echo "=========================================="

# 1. Check prerequisites
command -v java >/dev/null 2>&1 || { echo "Java 21+ is required but not installed. Aborting."; exit 1; }
command -v docker >/dev/null 2>&1 || { echo "Docker is required but not installed. Aborting."; exit 1; }
command -v mvn >/dev/null 2>&1 || { echo "Maven is required but not installed. Aborting."; exit 1; }

# 2. Start RabbitMQ
echo ""
echo "[1/3] Starting RabbitMQ..."
docker-compose up -d rabbitmq

echo "[2/3] Waiting for RabbitMQ to be ready..."
until docker inspect -f '{{.State.Health.Status}}' scanner-rabbitmq 2>/dev/null | grep -q healthy; do
    echo "  Waiting for RabbitMQ..."
    sleep 2
done
echo "  RabbitMQ is ready! Management UI: http://localhost:15672 (guest/guest)"

# 3. Start the application
echo ""
echo "[3/3] Starting ms-scanner-service..."
mvn spring-boot:run -Dspring-boot.run.profiles=dev

echo ""
echo "Application stopped."