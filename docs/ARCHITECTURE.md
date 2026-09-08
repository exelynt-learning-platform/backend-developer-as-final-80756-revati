# Architecture

## Overview

Layered Spring Boot API for booking resources (rooms, vehicles, equipment) with JWT authentication and role-based access control.

```
Client → Controller → Service → Repository → Database
              ↓
         Security (JWT filter + RBAC)
```

## Package layout

| Package | Responsibility |
|---------|----------------|
| `controller` | REST endpoints, HTTP status codes |
| `service` | Business rules, ownership checks |
| `domain.entity` / `enums` / `repository` | JPA model and persistence |
| `dto.request` / `dto.response` | API contracts (no entity leakage) |
| `mapper` | Entity ↔ DTO conversion |
| `security` | JWT, `UserDetails`, security helpers |
| `config` | Security, OpenAPI, data seeding |
| `exception` | Domain exceptions + global handler |

## Roles

| Action | ADMIN | USER |
|--------|-------|------|
| Resource CRUD | Full | Read only |
| Create reservation | Yes (owner = JWT user) | Yes (owner = JWT user) |
| List/view reservations | All | Own only |
| Update reservation | Full | Own only; status limited to `CANCELLED` |
| Delete reservation | Yes | No |

Reservation ownership is **always** resolved from the authenticated JWT principal. Clients cannot assign `userId` in the request body.

## Reservation statuses

`PENDING` → `CONFIRMED` / `CANCELLED`

## Filtering & pagination

`GET /api/reservations` supports:

- `status` — `PENDING` \| `CONFIRMED` \| `CANCELLED`
- `minPrice` / `maxPrice` — decimal bounds
- `page` / `size` — pagination
- `sort` — e.g. `price,desc` or `createdAt,asc`

## Profiles

| Profile | Database |
|---------|----------|
| `h2` (default) | In-memory H2 |
| `postgres` | PostgreSQL |
| `mysql` | MySQL |
