# Resource Booking System

Secure RESTful Resource Booking API built with **Spring Boot 3.3**, **Java 17**, **Spring Security + JWT**, **JPA/Hibernate**, and **MySQL / PostgreSQL** (H2 for local/dev).

## Features

- JWT login via `POST /auth/login` (BCrypt password hashing, stateless sessions)
- RBAC with `ADMIN` and `USER` roles
- Resource CRUD (ADMIN write, USER read)
- Reservation CRUD with ownership from JWT (USER sees only own bookings)
- Statuses: `PENDING`, `CONFIRMED`, `CANCELLED`
- Decimal reservation pricing
- Filter by `status`, `minPrice`, `maxPrice`
- Pagination (`page`, `size`) and optional `sort`
- Bean Validation + structured error responses
- Swagger UI + Postman collection
- Seed users and sample resources

## Architecture

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for package layout and permission matrix.

```
src/main/java/com/booking/
├── controller/     # REST endpoints
├── service/        # Business logic & ownership rules
├── domain/         # Entities, enums, repositories
├── dto/            # Request/response contracts
├── mapper/         # Entity ↔ DTO
├── security/       # JWT filter, UserDetails, helpers
├── config/         # Security, OpenAPI, DataSeeder
└── exception/      # Global error handling
```

## Prerequisites

- JDK 17+
- Maven 3.8+
- Optional: PostgreSQL 14+ or MySQL 8+ (default profile uses H2)

## Quick start (H2 — no external DB)

```bash
mvn spring-boot:run
```

App starts on `http://localhost:8080` with profile `h2`.

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- H2 console: http://localhost:8080/h2-console  
  JDBC URL: `jdbc:h2:mem:bookingdb`

## Seed users

| Username | Password   | Role  |
|----------|------------|-------|
| `admin`  | `Admin@123`| ADMIN |
| `user`   | `User@123` | USER  |
| `alice`  | `Alice@123`| USER  |

Sample resources (Conference Room A, Company Van, DSLR Camera Kit) are inserted on first startup.

## Environment variables

See [.env.example](.env.example).

| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | `h2` \| `postgres` \| `mysql` | `h2` |
| `SERVER_PORT` | HTTP port | `8080` |
| `JWT_SECRET` | HMAC signing secret (≥ 32 chars) | dev placeholder |
| `JWT_EXPIRATION_MS` | Token lifetime in ms | `86400000` (24h) |
| `DB_HOST` | DB host | `localhost` |
| `DB_PORT` | DB port | `5432` / `3306` |
| `DB_NAME` | Database name | `bookingdb` |
| `DB_USERNAME` | DB user | profile-specific |
| `DB_PASSWORD` | DB password | profile-specific |

## PostgreSQL setup

```bash
createdb bookingdb
export SPRING_PROFILES_ACTIVE=postgres
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=bookingdb
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
export JWT_SECRET='your-production-grade-secret-key-here'
mvn spring-boot:run
```

## MySQL setup

```bash
mysql -e "CREATE DATABASE bookingdb;"
export SPRING_PROFILES_ACTIVE=mysql
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=bookingdb
export DB_USERNAME=root
export DB_PASSWORD=root
export JWT_SECRET='your-production-grade-secret-key-here'
mvn spring-boot:run
```

## API overview

### Authentication

```http
POST /auth/login
Content-Type: application/json

{ "username": "admin", "password": "Admin@123" }
```

Response includes `accessToken`. Send it as:

```http
Authorization: Bearer <accessToken>
```

### Resources

| Method | Path | ADMIN | USER |
|--------|------|-------|------|
| GET | `/api/resources` | ✓ | ✓ |
| GET | `/api/resources/{id}` | ✓ | ✓ |
| POST | `/api/resources` | ✓ | ✗ |
| PUT | `/api/resources/{id}` | ✓ | ✗ |
| DELETE | `/api/resources/{id}` | ✓ | ✗ |

Query: `page`, `size`, `sort` (e.g. `name,asc`)

### Reservations

| Method | Path | ADMIN | USER |
|--------|------|-------|------|
| GET | `/api/reservations` | all | own |
| GET | `/api/reservations/{id}` | all | own |
| POST | `/api/reservations` | ✓ | ✓ |
| PUT | `/api/reservations/{id}` | ✓ | own (cancel/notes/times) |
| DELETE | `/api/reservations/{id}` | ✓ | ✗ |

Create body (no `userId` — owner comes from JWT):

```json
{
  "resourceId": 1,
  "startTime": "2026-12-01T10:00:00",
  "endTime": "2026-12-01T12:00:00",
  "price": 150.00,
  "notes": "Kickoff meeting"
}
```

List filters:

```http
GET /api/reservations?status=PENDING&minPrice=50&maxPrice=200&page=0&size=10&sort=price,desc
```

## API documentation

1. **Swagger UI** — http://localhost:8080/swagger-ui.html (Authorize with Bearer token)
2. **Postman** — import [docs/postman/Resource-Booking-System.postman_collection.json](docs/postman/Resource-Booking-System.postman_collection.json)

Suggested Postman flow: Login Admin → Login User → Create Resource → Create Reservation → Filter Reservations.

## Running tests

```bash
mvn test
```

Coverage includes:

- JWT generation/validation
- Reservation ownership from JWT
- Integration tests for login, RBAC, filtering, and validation

## Example cURL

```bash
# Login
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"user","password":"User@123"}' | jq -r .accessToken)

# List resources
curl -s http://localhost:8080/api/resources?page=0&size=10 \
  -H "Authorization: Bearer $TOKEN" | jq

# Create reservation
curl -s -X POST http://localhost:8080/api/reservations \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "resourceId": 1,
    "startTime": "2026-12-01T10:00:00",
    "endTime": "2026-12-01T12:00:00",
    "price": 75.00,
    "notes": "Demo booking"
  }' | jq
```

## Commit history (build phases)

1. Scaffold Spring Boot + multi-DB profiles  
2. Domain entities, enums, repositories  
3. JWT authentication + RBAC security  
4. Resource CRUD APIs  
5. Reservation APIs with ownership & filters  
6. Error handling, seed data, OpenAPI  
7. Tests, Postman collection, documentation  

## License

Assignment / educational use.
