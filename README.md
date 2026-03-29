# my-bank-app

Учебный проект — банковское приложение на микросервисной архитектуре.  
Реализован в рамках **Sprint 9** курса Яндекс Практикум (Java-разработчик).

---

## Архитектура

```
Browser
  └── front-ui :8081  (Spring MVC + Thymeleaf, OAuth2 Authorization Code)
        └── gateway :8080  (Spring Cloud Gateway WebFlux, OAuth2 Resource Server)
              ├── accounts :8082  (REST, JPA/PostgreSQL, Liquibase)
              ├── cash     :8083  (REST, Feign → accounts, notifications)
              └── transfer :8084  (REST, Feign → accounts, notifications)
                    └── notifications :8085  (REST, OAuth2 Resource Server)

Infrastructure:
  discovery :8761  (Eureka Server)
  config    :8888  (Spring Cloud Config Server)
  keycloak  :8180  (Keycloak 26, realm my-bank)
  postgres  :5432  (PostgreSQL 16)
```

### Микросервисы

| Сервис        | Порт | Описание                                                  |
|---------------|------|-----------------------------------------------------------|
| discovery     | 8761 | Eureka Server — реестр сервисов                           |
| config        | 8888 | Config Server — централизованная конфигурация             |
| gateway       | 8080 | API Gateway с circuit breaker (Resilience4j) и TokenRelay |
| front-ui      | 8081 | Веб-интерфейс пользователя (Thymeleaf)                    |
| accounts      | 8082 | Управление счетами, депозит/снятие                        |
| cash          | 8083 | Кассовые операции (вносит/снимает наличные)               |
| transfer      | 8084 | Переводы между счетами                                    |
| notifications | 8085 | Сервис уведомлений                                        |

---

## Технологии

- **Java 21**, **Spring Boot 4.0.4**, **Spring Cloud 2025.1.1**
- **Spring Security** — OAuth2 Authorization Code (front-ui) + Client Credentials (сервис-to-сервис)
- **Keycloak 26** — Identity Provider, realm `my-bank`
- **OpenFeign** — межсервисное взаимодействие с OAuth2 токенами
- **Spring Cloud Gateway** — WebFlux, circuit breaker, TokenRelay
- **PostgreSQL 16** + **Liquibase** — хранение данных accounts
- **Eureka** — service discovery
- **Gradle** (Groovy DSL), многомодульный проект

---

## Безопасность

```
Authorization Code Flow:
  Browser → front-ui → Keycloak → front-ui (получает токен пользователя)
  front-ui → gateway (Bearer token) → business services

Client Credentials Flow (сервис-to-сервис):
  accounts → notifications  (scope: notifications.write)
  cash     → accounts       (scope: accounts.write)
  cash     → notifications  (scope: notifications.write)
  transfer → accounts       (scope: accounts.write)
  transfer → notifications  (scope: notifications.write)
```

Каждый бизнес-сервис защищён как **Resource Server** (JWT / jwk-set-uri).  
Scope-based авторизация: `accounts.read`, `accounts.write`, `cash.write`, `transfer.write`, `notifications.write`.

---

## Быстрый старт

### Требования

- Docker и Docker Compose
- JDK 21

### Сборка и запуск

```bash
# Полная сборка
./gradlew clean build -x test

# Запуск всей инфраструктуры
docker compose up --build -d
```

> **Примечание:** сервисы `discovery`, `config`, `keycloak`, `postgres` поднимаются автоматически и имеют healthcheck.  
> Бизнес-сервисы (`accounts`, `cash`, `transfer`, `notifications`, `gateway`, `front-ui`) запускаются после того, как инфраструктура станет healthy.

### Пересборка отдельного сервиса

```bash
./gradlew :<service>:clean :<service>:build -x test \
  && docker compose stop <service> \
  && docker compose rm -f <service> \
  && docker compose up <service> --build -d
```

Пример для `accounts`:
```bash
./gradlew :accounts:clean :accounts:build -x test \
  && docker compose stop accounts \
  && docker compose rm -f accounts \
  && docker compose up accounts --build -d
```

---

## Доступные URL

| Сервис        | URL                                 |
|---------------|-------------------------------------|
| Веб-интерфейс | http://localhost:8081               |
| API Gateway   | http://localhost:8080               |
| Keycloak      | http://localhost:8180               |
| Eureka        | http://localhost:8761               |
| Config Server | http://localhost:8888/actuator/health |

### Тестовые пользователи (Keycloak realm `my-bank`)

| Пользователь | Пароль   | Роль  |
|--------------|----------|-------|
| ivan         | password | USER  |
| petrov       | password | USER  |

---

## Структура проекта

```
my-bank-app/
├── build.gradle          # корневой — общие зависимости и плагины
├── settings.gradle
├── docker-compose.yaml
├── discovery/            # Eureka Server
├── config/               # Config Server
│   └── src/main/resources/configs/  # конфиги сервисов
├── gateway/              # API Gateway (WebFlux)
├── front-ui/             # Веб-интерфейс (MVC + Thymeleaf)
├── accounts/             # Сервис счетов (JPA + Liquibase)
├── cash/                 # Кассовый сервис
├── transfer/             # Сервис переводов
├── notifications/        # Сервис уведомлений
├── keycloak/             # Realm export для автоимпорта
└── postgres/             # init.sql для инициализации БД
```

---

## API

### accounts (8082)

| Метод | URL                          | Scope needed       | Описание                  |
|-------|------------------------------|--------------------|---------------------------|
| GET   | /api/accounts/me             | accounts.read      | Мой счёт                  |
| PUT   | /api/accounts/me             | accounts.write     | Обновить профиль          |
| GET   | /api/accounts                | accounts.read      | Все счета                 |
| POST  | /api/accounts/{id}/deposit   | accounts.write     | Пополнить счёт            |
| POST  | /api/accounts/{id}/withdraw  | accounts.write     | Снять со счёта            |

### cash (8083)

| Метод | URL         | Scope needed | Описание           |
|-------|-------------|--------------|--------------------|
| POST  | /api/cash/** | cash.write   | Кассовые операции  |

### transfer (8084)

| Метод | URL              | Scope needed   | Описание       |
|-------|------------------|----------------|----------------|
| POST  | /api/transfer/** | transfer.write | Переводы       |

---

## Тесты

```bash
# Все тесты
./gradlew test

# Тесты конкретного модуля
./gradlew :accounts:test
```