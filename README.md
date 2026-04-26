# my-bank-app

Учебный проект — банковское приложение на микросервисной архитектуре.  
Реализован в рамках **Sprint 11** курса Яндекс Практикум (Java-разработчик).  
Развёртывание микросервисов в **Kubernetes** с использованием **Helm-чартов**.  
Взаимодействие с сервисом уведомлений через **Apache Kafka**.

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
              ├── cash     :8083  (REST, Feign → accounts)
              └── transfer :8084  (REST, Feign → accounts)

              accounts ──┐
              cash     ──┼── Kafka (топик: notifications) ──► notifications :8085
              transfer ──┘

Infrastructure (Kubernetes):
  PostgreSQL StatefulSet :5432
  Kafka StatefulSet      :9092  (KRaft mode, single node, PVC)
  Keycloak Deployment    :8080  (NodePort 30180)
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
| notifications | 8085 | ClusterIP   | Сервис уведомлений (Kafka consumer)                       |
| kafka         | 9092 | ClusterIP   | Apache Kafka (KRaft, single node, StatefulSet)            |
| keycloak      | 8080 | NodePort 30180 | OAuth 2.0 сервер авторизации                           |
| postgres      | 5432 | ClusterIP   | PostgreSQL 16 (StatefulSet)                               |

### Изменения по сравнению со Sprint 10

- **Apache Kafka**: добавлена распределённая платформа для асинхронного обмена сообщениями
- **Notifications**: REST API заменён на Kafka consumer (`@KafkaListener`)
- **Accounts, Cash, Transfer**: Feign-клиент к Notifications заменён на Kafka producer (`KafkaTemplate`)
- **OAuth2 Client Credentials**: удалены из accounts (больше не нужен для вызова notifications); из cash/transfer убран scope `notifications.write`
- **Kafka Helm-сабчарт**: StatefulSet с PVC для персистентности данных топиков
- **Топик `notifications`**: single partition, replication factor 1, at-least-once delivery

---

## Технологии

- **Java 21**, **Spring Boot 4.0.4**, **Spring Cloud 2025.1.1**
- **Apache Kafka 3.9.0** (KRaft mode) + **Spring Kafka 4.0.4**
- **Spring Security** — OAuth2 Authorization Code (front-ui) + Client Credentials (сервис-to-сервис)
- **Keycloak 26** — Identity Provider, realm `my-bank`
- **OpenFeign** — межсервисное взаимодействие accounts ← cash/transfer (с OAuth2 токенами)
- **Spring Cloud Gateway** — WebFlux, circuit breaker, TokenRelay
- **PostgreSQL 16** + **Liquibase** — хранение данных accounts
- **Kubernetes** + **Helm** — оркестрация и пакетный менеджер
- **Gradle** (Groovy DSL), многомодульный проект

---

## Apache Kafka

### Конфигурация

- **Режим**: KRaft (без ZooKeeper), single node
- **Топик**: `notifications` (1 partition, 1 replica)
- **Сериализация**: JSON (`JsonSerializer` / `JsonDeserializer`)
- **Семантика доставки**: at-least-once (Spring Kafka AckMode.BATCH, auto.commit=false)
- **Порядок сообщений**: не гарантируется (unordered)
- **Персистентность**: PersistentVolumeClaim — данные топиков сохраняются при перезапуске подов
- **Consumer group**: `notifications-group` — при рестарте notifications продолжает с последнего коммита offset

### Продюсеры

Сервисы **accounts**, **cash** и **transfer** публикуют JSON-сообщения в топик `notifications`:

```json
{
  "message": "Счёт ivan пополнен на 100 руб. Текущий баланс: 1100 руб."
}
```

### Консьюмер

Сервис **notifications** читает сообщения через `@KafkaListener` и логирует их.

---

## Безопасность

```
Authorization Code Flow:
  Browser → front-ui → Keycloak (NodePort 30180) → front-ui (получает токен пользователя)
  front-ui → gateway (Bearer token) → business services

Client Credentials Flow (сервис-to-сервис):
  cash     → accounts  (scope: accounts.write)
  transfer → accounts  (scope: accounts.write)

Kafka (без OAuth2):
  accounts, cash, transfer → Kafka → notifications
```

Каждый бизнес-сервис защищён как **Resource Server** (JWT / jwk-set-uri).  
Scope-based авторизация: `accounts.read`, `accounts.write`, `cash.write`, `transfer.write`.

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
│       ├── test-kafka.yaml
│       ├── test-keycloak.yaml
│       ├── test-accounts.yaml
│       ├── test-cash.yaml
│       ├── test-transfer.yaml
│       ├── test-notifications.yaml
│       ├── test-gateway.yaml
│       └── test-front-ui.yaml
└── charts/                      # сабчарты
    ├── postgres/                # StatefulSet + Service
    ├── kafka/                   # StatefulSet + Service (KRaft, PVC)
    ├── keycloak/                # Deployment + NodePort Service + ConfigMap (realm)
    ├── accounts/                # Deployment + ClusterIP Service + ConfigMap
    ├── cash/                    # Deployment + ClusterIP Service + ConfigMap
    ├── transfer/                # Deployment + ClusterIP Service + ConfigMap
    ├── notifications/           # Deployment + ClusterIP Service + ConfigMap
    ├── gateway/                 # Deployment + ClusterIP Service + ConfigMap
    └── front-ui/                # Deployment + NodePort Service + ConfigMap
```

### Helm-тесты

```bash
helm test my-bank
```

Тесты проверяют TCP-доступность каждого сервиса внутри кластера.

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
├── helm/                 # Helm-чарты
├── gateway/              # API Gateway (WebFlux)
├── front-ui/             # Веб-интерфейс (MVC + Thymeleaf)
├── accounts/             # Сервис счетов (JPA + Liquibase + Kafka producer)
├── cash/                 # Кассовый сервис (Feign + Kafka producer)
├── transfer/             # Сервис переводов (Feign + Kafka producer)
├── notifications/        # Сервис уведомлений (Kafka consumer)
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

### Unit-тесты

- **AccountServiceTest** — CRUD-операции, проверка отправки уведомлений в Kafka
- **CashServiceTest** — deposit/withdraw с проверкой вызова Kafka producer
- **TransferServiceTest** — перевод с saga-компенсацией и Kafka producer
- **AccountControllerTest, CashControllerTest, TransferControllerTest** — JWT-авторизация, валидация

### Интеграционные тесты

- **AccountServiceIntegrationTest** — Testcontainers (PostgreSQL), полный цикл операций
- **NotificationKafkaListenerTest** — EmbeddedKafka, проверка получения и обработки сообщений

### Testcontainers (Rancher Desktop)

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
