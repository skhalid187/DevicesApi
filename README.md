# Devices API

A PostgreSQL-backed REST API for creating, viewing, updating, deleting, filtering, and paginating devices.

## Tech stack

Java 21, Spring Boot 3.5 (Web, Data JPA, Validation), PostgreSQL 16, Flyway, springdoc OpenAPI, Maven, Docker Compose, JUnit 5, Mockito, Testcontainers, and JaCoCo.

## Design and implementation

Controllers handle HTTP and request validation, the service handles business rules and transactions, repositories handle persistence, and DTOs define API input and output.

Updates and deletes use a pessimistic write lock within a transaction. This serializes concurrent changes to the same device while its state rules are checked. Reads use read-only transactions.

Flyway manages the schema and database constraints. PostgreSQL generates an immutable `creationTime`; lists are sorted by `id`. Errors use HTTP Problem Details.

## Validation and business rules

- `name` and `brand` are required, nonblank, and at most 100 characters for create and replace. Supplied values in a patch follow the same rules.
- `state` is required for create and replace. Its accepted JSON values are exactly `available`, `in-use`, and `inactive` (case sensitive).
- A patch requires at least one non-null editable field; omitted fields retain their values. `id` and `creationTime` are server managed and read only. IDs must be positive.
- An `in-use` device cannot be renamed, rebranded, or deleted. Its state can change; other changes are allowed after it leaves `in-use`.
- Listing supports an exact, case insensitive `brand` filter (1–100 characters after trimming) and a `state` filter. `page` starts at `0` (default `0`); `size` is `1`–`100` (default `20`).

Invalid requests return `400`, missing devices return `404`, and violations of an `in-use` rule return `409`.

## Run locally

You need Java 21 and PostgreSQL. The included Maven wrapper provides Maven 3.9+. Create a database and user using a PostgreSQL administrator account:

```sql
CREATE ROLE devices_user LOGIN PASSWORD 'change_me';
CREATE DATABASE devices_api OWNER devices_user;
```

Then, from the project root:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/devices_api
export DB_USERNAME=devices_user
export DB_PASSWORD=change_me
./mvnw spring-boot:run
```

Flyway migrates the database at startup. The API runs at `http://localhost:8080` by default; `SERVER_PORT` changes that port.

## Run with Docker Compose

With Docker and Compose installed, run:

```bash
cp .env.example .env
docker compose up --build -d
```

The API runs at `http://localhost:8080`; PostgreSQL data persists in the `devices-data` volume. Change credentials or host ports (`API_PORT`, `DB_PORT`) in `.env`. This file is read by Compose, not by a direct Spring Boot run. Stop the containers with `docker compose down`.

## API documentation

With the API running, use [Swagger UI](http://localhost:8080/swagger-ui/index.html) or the [OpenAPI JSON](http://localhost:8080/v3/api-docs). Adjust the port in these links if needed.

## Tests and coverage

With Docker running, execute:

```bash
./mvnw clean verify
```

The suite includes unit and controller tests plus PostgreSQL integration tests using Testcontainers. JaCoCo writes the coverage report to `target/site/jacoco/index.html` and requires at least **80% line coverage** during `verify`.
