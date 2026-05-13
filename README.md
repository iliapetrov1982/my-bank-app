# my-bank-app
Educational project — a banking application built on a microservices architecture.  
Implemented as part of **Sprint 12** of the Yandex Practicum course (Java developer).  
Microservices are deployed to **Kubernetes** using **Helm charts**.  
Interaction with the notifications service is done through **Apache Kafka**.  
Distributed tracing (**Zipkin**), metrics (**Prometheus + Grafana**) and logging (**ELK**).

---

## Architecture

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
              cash     ──┼── Kafka (topic: notifications) ──► notifications :8085
              transfer ──┘

Infrastructure (Kubernetes):
  PostgreSQL StatefulSet :5432
  Kafka StatefulSet      :9092  (KRaft mode, single node, PVC)
  Keycloak Deployment    :8080  (NodePort 30180)
  ConfigMaps + Secrets   (instead of Eureka + Config Server)

Observability stack (Kubernetes, in-memory):
  Zipkin        :9411  (NodePort 30411) — distributed tracing
  Prometheus    :9090  (NodePort 30909) — metric collection and alerts
  Grafana       :3000  (NodePort 30300) — dashboards
  Elasticsearch :9200  (ClusterIP)      — log storage
  Logstash      :5000  (ClusterIP, TCP) — log processing (input from microservices)
  Kibana        :5601  (NodePort 30561) — log UI
```

### Microservices

| Service       | Port | K8s Service    | Description                                                |
|---------------|------|----------------|------------------------------------------------------------|
| gateway       | 8080 | ClusterIP      | API Gateway with circuit breaker (Resilience4j) and TokenRelay |
| front-ui      | 8081 | NodePort 30081 | User-facing web interface (Thymeleaf)                      |
| accounts      | 8082 | ClusterIP      | Account management, deposit/withdraw                       |
| cash          | 8083 | ClusterIP      | Cash operations (deposit/withdraw cash)                    |
| transfer      | 8084 | ClusterIP      | Transfers between accounts                                 |
| notifications | 8085 | ClusterIP      | Notifications service (Kafka consumer)                     |
| kafka         | 9092 | ClusterIP      | Apache Kafka (KRaft, single node, StatefulSet)             |
| keycloak      | 8080 | NodePort 30180 | OAuth 2.0 authorization server                             |
| postgres      | 5432 | ClusterIP      | PostgreSQL 16 (StatefulSet)                                |

### Changes compared to Sprint 10

- **Apache Kafka**: a distributed platform for asynchronous messaging has been added
- **Notifications**: the REST API has been replaced by a Kafka consumer (`@KafkaListener`)
- **Accounts, Cash, Transfer**: the Feign client to Notifications has been replaced by a Kafka producer (`KafkaTemplate`)
- **OAuth2 Client Credentials**: removed from accounts (no longer needed for calling notifications); the `notifications.write` scope has been removed from cash/transfer
- **Kafka Helm subchart**: StatefulSet with a PVC for persistence of topic data
- **Topic `notifications`**: single partition, replication factor 1, at-least-once delivery

### Changes compared to Sprint 11

- **Zipkin**: distributed tracing via `spring-boot-starter-zipkin` (Spring Boot 4). Covers cross-service HTTP, Kafka producer/consumer (with propagation through Kafka headers) and JDBC in `accounts` (`datasource-micrometer-spring-boot`). The Front UI `RestClient` is configured with an `ObservationRegistry`, so the user-driven HTTP flow ends up in the same trace.
- **Prometheus + Grafana**: metric collection (`/actuator/prometheus`), HTTP/JVM/business dashboards, alerts. JDBC metrics are also collected (`jdbc_connection_*`, `jdbc_connections_active/idle/max`).
- **ELK stack**: Elasticsearch + Logstash + Kibana. Logs are sent via a TCP appender (logstash-logback-encoder), JSON format with MDC `traceId`/`spanId` to link with Zipkin
- **Business metrics**: `cash.withdraw.failed{login}`, `transfer.failed{from_login,to_login}`, `notification.send.failed{login}`
- **NotificationEvent**: a `login` field has been added so that business metrics and logs can be grouped per user
- **`/actuator/prometheus`** is exposed without authentication (inside the cluster) for scraping
- **Notifications consumer**: `JacksonJsonDeserializer.setUseTypeHeaders(false)` — ignores the FQN in the `__TypeId__` Kafka header (producers in different modules have different FQNs for `NotificationEvent`)

---

## Technologies

- **Java 21**, **Spring Boot 4.0.4**, **Spring Cloud 2025.1.1**
- **Apache Kafka 3.9.0** (KRaft mode) + **Spring Kafka 4.0.4**
- **Spring Security** — OAuth2 Authorization Code (front-ui) + Client Credentials (service-to-service)
- **Keycloak 26** — Identity Provider, realm `my-bank`
- **OpenFeign** — inter-service communication accounts ← cash/transfer (with OAuth2 tokens)
- **Spring Cloud Gateway** — WebFlux, circuit breaker, TokenRelay
- **PostgreSQL 16** + **Liquibase** — data storage for accounts
- **Kubernetes** + **Helm** — orchestration and package manager
- **Gradle** (Groovy DSL), multi-module project
- **Spring Boot Tracing** (`spring-boot-starter-zipkin`) + **Brave 6** + **Zipkin 3** — distributed tracing (HTTP, Kafka, JDBC)
- **datasource-micrometer-spring-boot 2.2.1** — JDBC instrumentation (`connection/query/result-set` spans, `jdbc_*` metrics)
- **Micrometer + Spring Boot Actuator** + **Prometheus 3** + **Grafana 11** — metrics and dashboards
- **Elasticsearch 8.16** + **Logstash 8.16** + **Kibana 8.16** — centralized logging
- **logstash-logback-encoder 8** — shipping JSON logs from Logback to Logstash over TCP

---

## Apache Kafka

### Configuration

- **Mode**: KRaft (no ZooKeeper), single node
- **Topic**: `notifications` (1 partition, 1 replica)
- **Serialization**: JSON (`JsonSerializer` / `JsonDeserializer`)
- **Delivery semantics**: at-least-once (Spring Kafka AckMode.BATCH, auto.commit=false)
- **Message ordering**: not guaranteed (unordered)
- **Persistence**: PersistentVolumeClaim — topic data survives pod restarts
- **Consumer group**: `notifications-group` — on restart, notifications continues from the last committed offset

### Producers

The **accounts**, **cash** and **transfer** services publish JSON messages to the `notifications` topic:

```json
{
  "message": "Account ivan has been topped up by 100 rub. Current balance: 1100 rub."
}
```

### Consumer

The **notifications** service reads messages via `@KafkaListener` and logs them.

---

## Observability

All components are deployed as Helm subcharts inside the Kubernetes cluster; storage is in-memory (`emptyDir`).

### Distributed tracing (Zipkin)

- **URL**: http://localhost:30411
- **Starter**: `spring-boot-starter-zipkin` (Spring Boot 4 combined starter: Micrometer Tracing + Brave bridge + Zipkin reporter)
- **Sampling**: 1.0 (every request)
- **Endpoint**: `management.tracing.export.zipkin.endpoint` → `http://zipkin:9411/api/v2/spans`
- **What is traced**:
  - incoming/outgoing HTTP requests in all 6 services (servlet and WebFlux);
  - **Kafka producer/consumer** — `KafkaTemplate.setObservationEnabled(true) + setObservationRegistry(...)` in producers, `ContainerProperties.setObservationEnabled/Registry` in the consumer. Trace context is propagated through Kafka headers, so the producer span and the consumer span end up in the same trace;
  - **JDBC** in `accounts` — via `net.ttddyy.observation:datasource-micrometer-spring-boot:2.2.1`. It decorates the DataSource: each request shows `connection / query / result-set` spans;
  - **Front UI HTTP client** — `RestClient.Builder` is explicitly configured with an `ObservationRegistry`, so outgoing calls from the browser flow also carry a trace id and continue the chain into the gateway;
  - `traceId` is propagated between services via B3 headers (HTTP) and Kafka record headers.

In the Zipkin UI you can see the full chain of a single user action:
```
[front-ui]      SERVER   POST /transfer
[front-ui]      CLIENT   http get  ───────────────────────► [gateway → accounts]  (JDBC: connection/query/result-set)
[front-ui]      CLIENT   http post ───────────────────────► [gateway → transfer]
                                                                [transfer] PRODUCER notifications send
                                                                                    ▲
                                                              [notifications] CONSUMER notifications process
```

### Metrics (Prometheus + Grafana)

- **Prometheus**: http://localhost:30909
- **Grafana**: http://localhost:30300 (admin / admin)
- **Scrape**: every 15 seconds at `/actuator/prometheus` of all 6 services (job_name = service name)

Prometheus and the Grafana datasource are configured automatically (provisioning).

#### `My Bank Overview` dashboard (Grafana)

| Group    | Panels |
|----------|--------|
| HTTP     | RPS, 5xx error rate, 4xx error rate, latency p50/p95/p99 |
| JVM      | Heap used, Process CPU, Live threads, GC pause time |
| Business | `cash.withdraw.failed{login}`, `transfer.failed{from_login,to_login}`, `notification.send.failed{login}` |

#### Prometheus alerts (`prometheus-alerts` ConfigMap)

| Group    | Alert | Condition |
|----------|-------|-----------|
| http     | HighHttp5xxRate | sum 5xx rate > 0.5 req/s for 1m |
| http     | HighRequestLatencyP95 | p95 latency > 1s for 5m |
| jvm      | HighJvmHeapUsage | heap > 85% for 5m |
| business | ManyFailedCashWithdrawals | rate > 0.1/s per login for 2m |
| business | ManyFailedTransfers | rate > 0.1/s per from_login for 2m |
| business | NotificationDeliveryFailures | total rate > 0.1/s for 2m |

View active alerts: http://localhost:30909/alerts.

#### Custom business metrics

Implemented through `MeterRegistry.counter(...)` in catch blocks of the services:

```java
// CashService.withdraw
catch (RuntimeException e) {
    meterRegistry.counter("cash.withdraw.failed", "login", login).increment();
    throw e;
}

// TransferService.transfer
catch (RuntimeException e) {
    meterRegistry.counter("transfer.failed",
            "from_login", fromLogin, "to_login", request.toLogin()).increment();
    throw e;
}

// NotificationService.notify
catch (RuntimeException e) {
    meterRegistry.counter("notification.send.failed", "login", event.login()).increment();
    throw e;
}
```

In Prometheus, the names are exposed with a `_total` suffix: `cash_withdraw_failed_total`, etc.

### Logging (ELK)

- **Kibana**: http://localhost:30561
- **Logstash**: TCP `5000` (json_lines codec)
- **Elasticsearch**: index `my-bank-YYYY.MM.dd`

Each service contains a `logback-spring.xml` with two appenders:
- `CONSOLE` — standard pattern with `[appName,traceId,spanId]` for local debugging;
- `LOGSTASH` — `LogstashTcpSocketAppender` with `LogstashEncoder` (JSON), includes MDC `traceId`/`spanId` and the customField `application`.

`Micrometer Tracing` automatically puts `traceId`/`spanId` into MDC, so in Kibana you can use `traceId` to find all logs of a single request and correlate them with the trace in Zipkin.

#### Logstash filters (masking)

```
"message", "(?i)(\"?password\"?\s*[:=]\s*\")[^\"]*", "\1***"
"message", "(?i)(\"?account[_-]?number\"?\s*[:=]\s*\")[^\"]*", "\1***"
"message", "\b(\d{4})\d{8}(\d{4})\b", "\1********\2"      # card number mask
```

#### Creating an index pattern in Kibana

On first login:
1. Stack Management → Data Views → Create data view
2. Name: `my-bank`, Index pattern: `my-bank-*`, Timestamp field: `@timestamp`
3. Save → go to Discover

Every record has the fields `application`, `traceId`, `spanId`, `level`, `logger_name`, `message` — you can filter and search by them.

---

## Security

```
Authorization Code Flow:
  Browser → front-ui → Keycloak (NodePort 30180) → front-ui (receives the user's token)
  front-ui → gateway (Bearer token) → business services

Client Credentials Flow (service-to-service):
  cash     → accounts  (scope: accounts.write)
  transfer → accounts  (scope: accounts.write)

Kafka (no OAuth2):
  accounts, cash, transfer → Kafka → notifications
```

Each business service is protected as a **Resource Server** (JWT / jwk-set-uri).  
Scope-based authorization: `accounts.read`, `accounts.write`, `cash.write`, `transfer.write`.

---

## Quick start (Kubernetes)

### Requirements

- Kubernetes cluster (Rancher Desktop / Minikube / Kind / Colima)
- Helm 3+
- JDK 21
- Docker

### Build and deploy

```bash
# 1. Build all JAR files
./gradlew clean build -x test

# 2. Build Docker images (for Minikube — use docker-env)
eval $(minikube docker-env)  # Minikube only

docker build -t my-bank-accounts:latest ./accounts
docker build -t my-bank-cash:latest ./cash
docker build -t my-bank-transfer:latest ./transfer
docker build -t my-bank-notifications:latest ./notifications
docker build -t my-bank-gateway:latest ./gateway
docker build -t my-bank-front-ui:latest ./front-ui

# 3. Deploy with Helm
helm install my-bank ./helm/my-bank

# 4. Check pod status
kubectl get pods -w

# 5. Run Helm tests
helm test my-bank
```

### Rebuilding a single service

```bash
./gradlew :<service>:clean :<service>:build -x test
docker build -t my-bank-<service>:latest ./<service>
kubectl rollout restart deployment/<service>
```

### Uninstall

```bash
helm uninstall my-bank
```

---

## Available URLs (Kubernetes)

| Service       | URL                                  |
|---------------|--------------------------------------|
| Web UI        | http://localhost:30081               |
| Keycloak      | http://localhost:30180               |
| Zipkin        | http://localhost:30411               |
| Prometheus    | http://localhost:30909               |
| Grafana       | http://localhost:30300 (admin/admin) |
| Kibana        | http://localhost:30561               |

### Test users (Keycloak realm `my-bank`)

| User   | Password | Role |
|--------|----------|------|
| ivan   | password | USER |
| petrov | password | USER |

---

## Helm charts

### Structure

```
helm/my-bank/                    # umbrella chart
├── Chart.yaml
├── values.yaml
├── templates/
│   ├── secrets.yaml             # shared Secret for all services
│   └── tests/                   # Helm connectivity tests
│       ├── test-postgres.yaml
│       ├── test-kafka.yaml
│       ├── test-keycloak.yaml
│       ├── test-accounts.yaml
│       ├── test-cash.yaml
│       ├── test-transfer.yaml
│       ├── test-notifications.yaml
│       ├── test-gateway.yaml
│       ├── test-front-ui.yaml
│       ├── test-zipkin.yaml
│       ├── test-prometheus.yaml
│       ├── test-grafana.yaml
│       ├── test-elasticsearch.yaml
│       ├── test-logstash.yaml
│       └── test-kibana.yaml
└── charts/                      # subcharts
    ├── postgres/                # StatefulSet + Service
    ├── kafka/                   # StatefulSet + Service (KRaft, PVC)
    ├── keycloak/                # Deployment + NodePort Service + ConfigMap (realm)
    ├── accounts/                # Deployment + ClusterIP Service + ConfigMap
    ├── cash/                    # Deployment + ClusterIP Service + ConfigMap
    ├── transfer/                # Deployment + ClusterIP Service + ConfigMap
    ├── notifications/           # Deployment + ClusterIP Service + ConfigMap
    ├── gateway/                 # Deployment + ClusterIP Service + ConfigMap
    ├── front-ui/                # Deployment + NodePort Service + ConfigMap
    ├── zipkin/                  # Deployment + NodePort Service (in-memory)
    ├── prometheus/              # Deployment + NodePort Service + ConfigMap (scrape + alerts)
    ├── grafana/                 # Deployment + NodePort Service + ConfigMap (datasource + dashboards)
    ├── elasticsearch/           # Deployment + ClusterIP Service (single-node, in-memory)
    ├── logstash/                # Deployment + ClusterIP Service + ConfigMap (TCP input → ES output)
    └── kibana/                  # Deployment + NodePort Service
```

### Helm tests

```bash
helm test my-bank
```

The tests verify TCP reachability of each service inside the cluster.

---

## CI/CD (Jenkins)

The repository root contains a `Jenkinsfile` with a declarative pipeline:

| Stage               | Description                                                 |
|---------------------|-------------------------------------------------------------|
| Checkout            | Cloning the repository                                      |
| Build               | `./gradlew clean build -x test`                             |
| Test                | `./gradlew test` + publishing JUnit reports                 |
| Docker Build & Push | Parallel build and push of images for all 6 services        |
| Helm Lint           | Helm chart validation (`helm lint`)                         |
| Deploy              | `helm upgrade --install` with the `BUILD_NUMBER` tag        |
| Helm Test           | Running Helm tests to verify service reachability           |

### Jenkins agent requirements

- JDK 21
- Docker
- Helm 3+
- kubectl with access to the target cluster

### Required Jenkins credentials

| ID                          | Type              | Description                       |
|-----------------------------|-------------------|-----------------------------------|
| docker-registry-url         | Secret text       | Docker registry address           |
| docker-registry-credentials | Username/Password | Docker registry login/password    |

---

## Project structure

```
my-bank-app/
├── build.gradle          # root — common dependencies and plugins
├── settings.gradle
├── helm/                 # Helm charts
├── gateway/              # API Gateway (WebFlux)
├── front-ui/             # Web UI (MVC + Thymeleaf)
├── accounts/             # Accounts service (JPA + Liquibase + Kafka producer)
├── cash/                 # Cash service (Feign + Kafka producer)
├── transfer/             # Transfer service (Feign + Kafka producer)
├── notifications/        # Notifications service (Kafka consumer)
├── keycloak/             # Realm export for auto-import
└── postgres/             # init.sql for DB initialization
```

---

## API

### accounts (8082)

| Method | URL                          | Scope needed   | Description       |
|--------|------------------------------|----------------|-------------------|
| GET    | /api/accounts/me             | accounts.read  | My account        |
| PUT    | /api/accounts/me             | accounts.write | Update profile    |
| GET    | /api/accounts                | accounts.read  | All accounts      |
| POST   | /api/accounts/{id}/deposit   | accounts.write | Deposit funds     |
| POST   | /api/accounts/{id}/withdraw  | accounts.write | Withdraw funds    |

### cash (8083)

| Method | URL          | Scope needed | Description     |
|--------|--------------|--------------|-----------------|
| POST   | /api/cash/** | cash.write   | Cash operations |

### transfer (8084)

| Method | URL              | Scope needed   | Description |
|--------|------------------|----------------|-------------|
| POST   | /api/transfer/** | transfer.write | Transfers   |

---

## Tests

```bash
# All tests
./gradlew test

# Tests for a specific module
./gradlew :accounts:test

# Helm tests
helm test my-bank
```

### Unit tests

- **AccountServiceTest** — CRUD operations, verification of notification publishing to Kafka
- **CashServiceTest** — deposit/withdraw with verification of the Kafka producer call
- **TransferServiceTest** — transfer with saga compensation and Kafka producer
- **AccountControllerTest, CashControllerTest, TransferControllerTest** — JWT authorization, validation

### Integration tests

- **AccountServiceIntegrationTest** — Testcontainers (PostgreSQL), full operation cycle
- **NotificationKafkaListenerTest** — EmbeddedKafka, verification of message receipt and processing

### Testcontainers (Rancher Desktop)

The `accounts` module contains integration tests that use **Testcontainers** (PostgreSQL).
A working Docker daemon is required to run them.

**Rancher Desktop:** the Ryuk component (a Testcontainers cleanup container) may fail to start because of a conflict with the Docker socket symlink. In `accounts/build.gradle` environment variables are set to work around this:

```groovy
test {
    environment 'TESTCONTAINERS_RYUK_DISABLED', 'true'
    environment 'DOCKER_HOST', 'unix:///Users/<username>/.rd/docker.sock'
}
```

When using **Docker Desktop** or **Colima**, these settings can be removed or `DOCKER_HOST` can be replaced with the actual socket path.