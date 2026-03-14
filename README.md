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
- Spring Security (form-based login, reactive)
- Keycloak 26.0 (OAuth2 Client Credentials for service-to-service auth)
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
- `market-app` uses form-based login (Spring Security) with users stored in PostgreSQL
- `payment-service` is an OAuth2 Resource Server; `market-app` authenticates via Keycloak Client Credentials

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
- **Keycloak** on port `8180` (OAuth2 authorization server)
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
docker compose up postgres redis keycloak payment-service -d
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
├── keycloak/
│   └── realm-export.json                # Keycloak realm + client config
├── market-app/                          # Main application
│   └── src/main/java/.../mymarketapp/
│       ├── config/                      # PaymentClient, Security config
│       ├── controller/                  # WebFlux controllers (Mono<String>)
│       ├── service/                     # Reactive business logic
│       ├── repository/                  # R2DBC repositories
│       ├── entity/                      # R2DBC entities
│       ├── security/                    # AppUserDetails (custom UserDetails)
│       ├── dto/                         # Data transfer objects (records)
│       └── mapper/                      # MapStruct mapper
└── payment-service/                     # Payment microservice
    └── src/main/java/.../paymentservice/
        ├── config/                      # Payment properties, Security config
        ├── controller/                  # Implements generated PaymentApi
        └── service/                     # In-memory balance (AtomicInteger)
```

## Authentication

- **Form login**: Spring Security default login page at `/login`
- **Predefined users**: `user1` / `user1pass`, `user2` / `user2pass`
- **Per-user data**: each user has their own cart and orders
- **Anonymous access**: can browse catalog (`/items`) and view item details (`/items/{id}`)
- **Service-to-service**: OAuth2 Client Credentials via Keycloak (market-app → payment-service)
- **Keycloak admin**: http://localhost:8180 (admin/admin)

## API Endpoints

### market-app (port 8080)

| Method | URL | Description |
|--------|-----|-------------|
| GET | `/items` | Browse items (public) |
| GET | `/items/{id}` | Item detail page (public) |
| POST | `/items` | Add/remove item to cart from catalog (requires login) |
| POST | `/items/{id}` | Add/remove item to cart from detail page (requires login) |
| GET | `/cart/items` | View cart with balance info (requires login) |
| POST | `/cart/items` | Update cart item (requires login) |
| POST | `/buy` | Pay and place order (requires login) |
| GET | `/orders` | View all orders (requires login) |
| GET | `/orders/{id}` | View order details (requires login) |
| GET | `/login` | Login page (Spring Security default) |
| POST | `/logout` | Logout, clears session (requires login) |

### payment-service (port 8081)

| Method | URL | Description |
|--------|-----|-------------|
| GET | `/api/payment/balance` | Get current balance |
| POST | `/api/payment/pay` | Process payment (deduct amount) |

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/market` | JDBC URL for Liquibase |
| `R2DB_URL` | `r2dbc:postgresql://localhost:5432/market` | R2DBC URL for app |
| `DB_USERNAME` | `postgres` | Database username |
| `DB_PASSWORD` | `postgres` | Database password |
| `REDIS_HOST` | `localhost` | Redis hostname |
| `REDIS_PORT` | `6379` | Redis port |
| `PAYMENT_SERVICE_URL` | `http://localhost:8081` | Payment service base URL |
| `PAYMENT_INITIAL_BALANCE` | `100000` | Initial balance for payment service |
| `KEYCLOAK_ISSUER_URI` | — | Keycloak issuer URI for OAuth2 resource server |
| `KEYCLOAK_TOKEN_URI` | — | Keycloak token endpoint for client credentials |
| `KEYCLOAK_CLIENT_SECRET` | — | Client secret for OAuth2 client credentials |
