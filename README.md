# my-bank-app

Учебный проект — банковское приложение на микросервисной архитектуре.  
Реализован в рамках **Sprint 10** курса Яндекс Практикум (Java-разработчик).  
Развёртывание микросервисов в **Kubernetes** с использованием **Helm-чартов**.

---

## Архитектура

```
Browser (http://localhost:30081)
  └── front-ui :8081  (Spring MVC + Thymeleaf, OAuth2 Authorization Code)
        │
        ├── keycloak :8080  (Keycloak 26, realm my-bank, NodePort 30180)
        │
        └── gateway :8080  (Spring Cloud Gateway WebFlux, OAuth2 Resource Server)
              ├── accounts :8082  (REST, JPA/PostgreSQL, Liquibase)
              ├── cash     :8083  (REST, Feign → accounts, notifications)
              └── transfer :8084  (REST, Feign → accounts, notifications)
                    └── notifications :8085  (REST, OAuth2 Resource Server)

Infrastructure (Kubernetes):
  PostgreSQL StatefulSet :5432
  Keycloak Deployment    :8080 (NodePort 30180)
  ConfigMaps + Secrets   (вместо Eureka + Config Server)
```

### Микросервисы

| Сервис        | Порт | K8s Service | Описание                                                  |
|---------------|------|-------------|-----------------------------------------------------------|
| gateway       | 8080 | ClusterIP   | API Gateway с circuit breaker (Resilience4j) и TokenRelay |
| front-ui      | 8081 | NodePort 30081 | Веб-интерфейс пользователя (Thymeleaf)                 |
| accounts      | 8082 | ClusterIP   | Управление счетами, депозит/снятие                        |
| cash          | 8083 | ClusterIP   | Кассовые операции (вносит/снимает наличные)               |
| transfer      | 8084 | ClusterIP   | Переводы между счетами                                    |
| notifications | 8085 | ClusterIP   | Сервис уведомлений                                        |
| keycloak      | 8080 | NodePort 30180 | OAuth 2.0 сервер авторизации                           |
| postgres      | 5432 | ClusterIP   | PostgreSQL 16 (StatefulSet)                               |

### Изменения по сравнению со Sprint 9

- **Service Discovery**: Kubernetes DNS (Service) вместо Eureka
- **Externalized Config**: ConfigMaps + Secrets вместо Spring Cloud Config Server
- **Gateway маршруты**: прямые HTTP URI вместо `lb://` (Eureka load balancer)
- **Развёртывание**: Helm-чарты (зонтичный + сабчарты) вместо docker-compose
- **PostgreSQL**: StatefulSet с PVC вместо Docker volume
- **Keycloak и Front-UI**: внутри Kubernetes-кластера, доступ через NodePort

---

## Технологии

- **Java 21**, **Spring Boot 4.0.4**, **Spring Cloud 2025.1.1**
- **Spring Security** — OAuth2 Authorization Code (front-ui) + Client Credentials (сервис-to-сервис)
- **Keycloak 26** — Identity Provider, realm `my-bank`
- **OpenFeign** — межсервисное взаимодействие с OAuth2 токенами
- **Spring Cloud Gateway** — WebFlux, circuit breaker, TokenRelay
- **PostgreSQL 16** + **Liquibase** — хранение данных accounts
- **Kubernetes** + **Helm** — оркестрация и пакетный менеджер
- **Gradle** (Groovy DSL), многомодульный проект

---

## Безопасность

```
Authorization Code Flow:
  Browser → front-ui → Keycloak (NodePort 30180) → front-ui (получает токен пользователя)
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

## Быстрый старт (Kubernetes)

### Требования

- Kubernetes кластер (Rancher Desktop / Minikube / Kind / Colima)
- Helm 3+
- JDK 21
- Docker

### Сборка и развёртывание

```bash
# 1. Собрать все JAR-файлы
./gradlew clean build -x test

# 2. Собрать Docker-образы (для Minikube — использовать docker-env)
eval $(minikube docker-env)  # только для Minikube

docker build -t my-bank-accounts:latest ./accounts
docker build -t my-bank-cash:latest ./cash
docker build -t my-bank-transfer:latest ./transfer
docker build -t my-bank-notifications:latest ./notifications
docker build -t my-bank-gateway:latest ./gateway
docker build -t my-bank-front-ui:latest ./front-ui

# 3. Развернуть с помощью Helm
helm install my-bank ./helm/my-bank

# 4. Проверить статус подов
kubectl get pods -w

# 5. Запустить Helm-тесты
helm test my-bank
```

### Пересборка отдельного сервиса

```bash
./gradlew :<service>:clean :<service>:build -x test
docker build -t my-bank-<service>:latest ./<service>
kubectl rollout restart deployment/<service>
```

### Удаление

```bash
helm uninstall my-bank
```

---

## Доступные URL (Kubernetes)

| Сервис        | URL                                 |
|---------------|-------------------------------------|
| Веб-интерфейс | http://localhost:30081              |
| Keycloak      | http://localhost:30180              |

### Тестовые пользователи (Keycloak realm `my-bank`)

| Пользователь | Пароль   | Роль  |
|--------------|----------|-------|
| ivan         | password | USER  |
| petrov       | password | USER  |

---

## Helm-чарты

### Структура

```
helm/my-bank/                    # зонтичный чарт
├── Chart.yaml
├── values.yaml
├── templates/
│   ├── secrets.yaml             # общий Secret для всех сервисов
│   └── tests/                   # Helm-тесты подключения
│       ├── test-postgres.yaml
│       ├── test-keycloak.yaml
│       ├── test-accounts.yaml
│       ├── test-cash.yaml
│       ├── test-transfer.yaml
│       ├── test-notifications.yaml
│       ├── test-gateway.yaml
│       └── test-front-ui.yaml
└── charts/                      # сабчарты
    ├── postgres/                # StatefulSet + Service
    ├── keycloak/                # Deployment + NodePort Service + ConfigMap (realm)
    ├── accounts/                # Deployment + ClusterIP Service + ConfigMap
    ├── cash/                    # Deployment + ClusterIP Service + ConfigMap
    ├── transfer/                # Deployment + ClusterIP Service + ConfigMap
    ├── notifications/           # Deployment + ClusterIP Service + ConfigMap
    ├── gateway/                 # Deployment + ClusterIP Service + ConfigMap
    └── front-ui/                # Deployment + NodePort Service + ConfigMap
```

### Развёртывание отдельного сабчарта

```bash
helm install accounts ./helm/my-bank/charts/accounts
```

### Helm-тесты

```bash
helm test my-bank
```

Тесты проверяют TCP/HTTP-доступность каждого сервиса внутри кластера.

---

## CI/CD (Jenkins)

В корне проекта находится `Jenkinsfile` с декларативным пайплайном:

| Stage               | Описание                                                    |
|---------------------|-------------------------------------------------------------|
| Checkout            | Клонирование репозитория                                    |
| Build               | `./gradlew clean build -x test`                             |
| Test                | `./gradlew test` + публикация JUnit-отчётов                |
| Docker Build & Push | Параллельная сборка и пуш образов для всех 6 сервисов      |
| Helm Lint           | Проверка Helm-чарта (`helm lint`)                           |
| Deploy              | `helm upgrade --install` с тегом `BUILD_NUMBER`             |
| Helm Test           | Запуск Helm-тестов для проверки доступности сервисов        |

### Требования к Jenkins-агенту

- JDK 21
- Docker
- Helm 3+
- kubectl с доступом к целевому кластеру

### Необходимые Credentials в Jenkins

| ID                          | Тип          | Описание                          |
|-----------------------------|--------------|-----------------------------------|
| docker-registry-url         | Secret text  | Адрес Docker-реестра              |
| docker-registry-credentials | Username/Password | Логин/пароль для Docker-реестра |

---

## Структура проекта

```
my-bank-app/
├── build.gradle          # корневой — общие зависимости и плагины
├── settings.gradle
├── docker-compose.yaml   # legacy (Sprint 9)
├── helm/                 # Helm-чарты (Sprint 10)
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

# Helm-тесты
helm test my-bank
```

### Интеграционные тесты и Testcontainers

Модуль `accounts` содержит интеграционные тесты с использованием **Testcontainers** (PostgreSQL).
Для их запуска необходим работающий Docker.

**Rancher Desktop:** компонент Ryuk (cleanup-контейнер Testcontainers) может не запускаться из-за конфликта с симлинком Docker-сокета. В `accounts/build.gradle` задаются переменные окружения для обхода этой проблемы:

```groovy
test {
    environment 'TESTCONTAINERS_RYUK_DISABLED', 'true'
    environment 'DOCKER_HOST', 'unix:///Users/<username>/.rd/docker.sock'
}
```

При использовании **Docker Desktop** или **Colima** эти настройки можно удалить или заменить `DOCKER_HOST` на актуальный путь к сокету.