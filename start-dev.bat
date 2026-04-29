@echo off
echo ==========================================
echo   Starting MS-Scanner-Service Dev Env
echo ==========================================

echo.
echo [1/3] Starting RabbitMQ...
docker-compose up -d rabbitmq

echo [2/3] Waiting for RabbitMQ to be ready...
:check_rabbitmq
timeout /t 2 /nobreak > nul
docker inspect -f "{{.State.Health.Status}}" scanner-rabbitmq 2>nul | findstr "healthy" > nul
if %errorlevel% neq 0 goto check_rabbitmq
echo   RabbitMQ is ready! Management UI: http://localhost:15672 (guest/guest)

echo.
echo [3/3] Starting ms-scanner-service...
mvn spring-boot:run -Dspring-boot.run.profiles=dev

echo.
echo Application stopped.
pause