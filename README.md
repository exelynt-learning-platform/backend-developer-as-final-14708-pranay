# Resource Booking System

Java 17 Spring Boot API for booking resources with PostgreSQL, Spring Data JPA, Spring Security, stateless JWT authentication, BCrypt password hashing, role-based access control, OpenAPI docs, tests, and a JaCoCo coverage gate.

## Run Locally

Start PostgreSQL:

```bash
docker compose up -d
```

Run the API:

```bash
mvn spring-boot:run
```

Useful environment variables:

| Variable | Default |
| --- | --- |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/resource_booking` |
| `SPRING_DATASOURCE_USERNAME` | `resource_booking` |
| `SPRING_DATASOURCE_PASSWORD` | `resource_booking` |
| `JWT_SECRET` | `change-me-change-me-change-me-change-me-change-me` |
| `JWT_EXPIRATION_MINUTES` | `120` |

Swagger UI is available at:

```text
http://localhost:8080/swagger-ui.html
```

Seed accounts are created outside the `test` profile:

| Role | Username | Password |
| --- | --- | --- |
| ADMIN | `admin` | `admin123` |
| USER | `user` | `user123` |

## Authentication

Login:

```http
POST /auth/login
Content-Type: application/json

{
  "username": "user",
  "password": "user123"
}
```

Use the returned token on protected requests:

```http
Authorization: Bearer <token>
```

Reservation ownership is always derived from the JWT principal. Reservation create/update requests do not accept a user id.

## Main Endpoints

Resources:

| Method | Path | Access |
| --- | --- | --- |
| GET | `/resources?page=0&size=20&sort=price,asc` | ADMIN, USER |
| GET | `/resources/{id}` | ADMIN, USER |
| POST | `/resources` | ADMIN |
| PUT | `/resources/{id}` | ADMIN |
| DELETE | `/resources/{id}` | ADMIN |

Reservations:

| Method | Path | Access |
| --- | --- | --- |
| GET | `/reservations` | ADMIN sees all, USER sees own |
| GET | `/reservations/{id}` | ADMIN sees all, USER sees own |
| POST | `/reservations` | ADMIN, USER |
| PUT | `/reservations/{id}` | ADMIN sees all, USER sees own |
| DELETE | `/reservations/{id}` | ADMIN sees all, USER sees own |

Reservation filters:

```text
/reservations?status=PENDING&minPrice=10.00&maxPrice=200.00&page=0&size=10&sort=startTime,desc
```

Paginated list endpoints return a stable response shape with `content`, `page`, `size`, `totalElements`, `totalPages`, `first`, and `last`.

Allowed resource sort fields: `id`, `name`, `price`.

Allowed reservation sort fields: `id`, `startTime`, `endTime`, `status`, `totalPrice`, `createdAt`.

## Validation And Error Responses

The API returns structured JSON errors with:

```json
{
  "timestamp": "2026-09-18T00:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "details": ["price: must be greater than 0.00"]
}
```

Reservation rules:

- `startTime` and `endTime` are required and must be in the future.
- `endTime` must be after `startTime`.
- Active reservations are `PENDING` and `CONFIRMED`.
- Active reservations for the same resource cannot overlap.
- `CANCELLED` reservations do not block overlapping active bookings.

## Tests And Coverage

Run the full verification suite:

```bash
mvn clean verify
```

The Maven build fails if JaCoCo line coverage is below 70%. The report is generated at:

```text
target/site/jacoco/index.html
```

The test profile uses H2 in PostgreSQL compatibility mode from `src/test/resources/application-test.yml`.
