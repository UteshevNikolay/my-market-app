# My Market App

## Tech Stack

- Java 21, Spring Boot 4.0.0
- Spring Data JPA, PostgreSQL 16
- Thymeleaf, Bootstrap 5
- Liquibase (DB migrations)
- Lombok, MapStruct
- Testcontainers (integration tests)

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
- **Application** on port `8080`

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

- **Unit tests** (25) — service layer with Mockito
- **Integration tests** (12) — controllers with Testcontainers PostgreSQL
- **Context test** (1) — Spring context loads

## Project Structure

```
src/main/java/com/my/project/mymarketapp/
├── controller/      # MVC controllers (Items, Cart, Orders)
├── service/         # Business logic
├── repository/      # Spring Data JPA repositories
├── entity/          # JPA entities (Item, CartItem, Order, OrderItem)
├── dto/             # Data transfer objects (records)
└── mapper/          # MapStruct mappers

src/main/resources/
├── templates/       # Thymeleaf templates (5 pages)
├── static/images/   # Product images
├── db/changelog/    # Liquibase migrations + seed data
└── application.yml  # Configuration
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
