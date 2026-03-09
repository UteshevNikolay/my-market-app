# My Market App

Online marketplace with product catalog, shopping cart, payment processing, and order management.

## Tech Stack

- Java 21, Spring Boot 4.0.0
- Spring WebFlux (reactive, non-blocking), Netty
- Spring Data R2DBC, PostgreSQL 16
- Redis 7 (reactive caching, cache-aside pattern)
- Thymeleaf, Bootstrap 5
- Liquibase (DB migrations, via JDBC)
- Lombok, MapStruct 1.6.3
- OpenAPI Generator 7.12.0 (client + server code generation)
- Testcontainers, WebTestClient, StepVerifier

## Architecture

Multi-module Gradle project with two Spring Boot applications:

| Module | Port | Purpose |
|--------|------|---------|
| `market-app` | 8080 | Main application (WebFlux + R2DBC + Thymeleaf) |
| `payment-service` | 8081 | REST payment service (WebFlux, in-memory balance) |

- `market-app` calls `payment-service` via a generated WebClient (OpenAPI client generation)
- `payment-service` implements a generated `PaymentApi` interface (OpenAPI server generation)
- Both sides are generated from a single `openapi/payment-api.yml` spec, enforcing the API contract at compile time

## Prerequisites

- Docker & Docker Compose
- Java 21 (only for local development without Docker)

## Quick Start (Docker)

Run the entire stack with a single command:

```bash
docker compose up --build -d
```

This starts:
- **PostgreSQL** on port `5432`
- **Redis** on port `6379`
- **payment-service** on port `8081`
- **market-app** on port `8080`

Open http://localhost:8080/items

To stop:

```bash
docker compose down
```

## Local Development

1. Start infrastructure and payment service:

```bash
docker compose up postgres redis payment-service -d
```

2. Run the main application:

```bash
./gradlew :market-app:bootRun
```

Open http://localhost:8080/items

## Build

```bash
./gradlew clean build              # Build all modules
./gradlew :market-app:bootJar      # Build market-app JAR
./gradlew :payment-service:bootJar # Build payment-service JAR
```

## Running Tests

Tests use Testcontainers, so Docker must be running.

```bash
./gradlew test                     # All modules (55 tests)
./gradlew :market-app:test         # market-app only (44 tests)
./gradlew :payment-service:test    # payment-service only (11 tests)
```

- **market-app**: 25 unit (Mockito + StepVerifier) + 19 integration (WebTestClient + Testcontainers)
- **payment-service**: 5 unit + 5 integration + 1 context load

## Project Structure

```
my-market-app/
├── openapi/
│   └── payment-api.yml                  # Shared OpenAPI 3.0.3 spec
├── market-app/                          # Main application
│   └── src/main/java/.../mymarketapp/
│       ├── config/                      # PaymentClient config
│       ├── controller/                  # WebFlux controllers (Mono<String>)
│       ├── service/                     # Reactive business logic
│       ├── repository/                  # R2DBC repositories
│       ├── entity/                      # R2DBC entities
│       ├── dto/                         # Data transfer objects (records)
│       └── mapper/                      # MapStruct mapper
└── payment-service/                     # Payment microservice
    └── src/main/java/.../paymentservice/
        ├── config/                      # Payment properties
        ├── controller/                  # Implements generated PaymentApi
        └── service/                     # In-memory balance (AtomicInteger)
```

## API Endpoints

### market-app (port 8080)

| Method | URL | Description |
|--------|-----|-------------|
| GET | `/items` | Browse items (search, sort, pagination) |
| GET | `/items/{id}` | Item detail page |
| POST | `/items` | Add/remove item to cart from catalog |
| POST | `/items/{id}` | Add/remove item to cart from detail page |
| GET | `/cart/items` | View cart (with balance info) |
| POST | `/cart/items` | Update cart item (increment/decrement/delete) |
| POST | `/buy` | Pay and place order |
| GET | `/orders` | View all orders |
| GET | `/orders/{id}` | View order details |

### payment-service (port 8081)

| Method | URL | Description |
|--------|-----|-------------|
| GET | `/api/payment/balance` | Get current balance |
| POST | `/api/payment/pay` | Process payment (deduct amount) |
