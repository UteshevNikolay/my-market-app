# My Market App

E-commerce marketplace where users browse products, manage a shopping cart, and place orders. Fully reactive stack powered by Spring WebFlux + R2DBC running on Netty.

## Tech Stack

- Java 21, Spring Boot 4.0.0
- Spring WebFlux (reactive, non-blocking), Netty
- Spring Data R2DBC, PostgreSQL 16
- Thymeleaf, Bootstrap 5
- Liquibase (DB migrations, via JDBC)
- Lombok, MapStruct 1.6.3
- Testcontainers, WebTestClient

## Prerequisites

- Docker & Docker Compose
- Java 21 (only for local development without Docker)

## Quick Start (Docker)

Run the entire application with a single command:

```bash
docker compose up --build -d
```

This starts:
- **PostgreSQL** on port `5432`
- **Application** on port `8080` (Netty)

Open http://localhost:8080/items

To stop:

```bash
docker compose down
```

## Local Development

1. Start only the database:

```bash
docker compose up postgres -d
```

2. Run the application:

```bash
./gradlew bootRun
```

Open http://localhost:8080/items

## Running Tests

Tests use Testcontainers, so Docker must be running.

```bash
./gradlew test
```

- **Unit tests** (25) — service layer with Mockito + StepVerifier
- **Integration tests** (12) — controllers with WebTestClient + Testcontainers PostgreSQL

## Project Structure

```
src/main/java/com/my/project/mymarketapp/
├── controller/      # WebFlux controllers (return Mono<String>)
├── service/         # Reactive business logic (Mono/Flux chains)
├── repository/      # Spring Data R2DBC repositories (ReactiveCrudRepository)
├── entity/          # R2DBC entities (raw FK IDs, no JPA relationships)
├── dto/             # Data transfer objects (records)
└── mapper/          # MapStruct mapper (ItemMapper)

src/main/resources/
├── templates/       # Thymeleaf templates (5 pages)
├── static/images/   # Product images
├── db/changelog/    # Liquibase migrations + seed data
└── application.yml  # Dual config: JDBC (Liquibase) + R2DBC (app)
```

## API Endpoints

| Method | URL | Description |
|--------|-----|-------------|
| GET | `/items` | Browse items (search, sort, pagination) |
| GET | `/items/{id}` | Item detail page |
| POST | `/items` | Add/remove item to cart from catalog |
| POST | `/items/{id}` | Add/remove item to cart from detail page |
| GET | `/cart/items` | View cart |
| POST | `/cart/items` | Update cart item (increment/decrement/delete) |
| POST | `/buy` | Place order from cart |
| GET | `/orders` | View all orders |
| GET | `/orders/{id}` | View order details |
