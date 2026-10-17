
# Architecture

Time tracking app: record hours per project, track budgets, warn when budgets run out.

## Stack


| Frontend | React 19 + TypeScript + Vite + Mantine 8 | SPA, responsive out of the box, Mantine gives forms/tables/modals |
| Backend | Spring Boot (Java 21), REST + JSON | Standard, strong validation, easy OpenAPI |
| DB | PostgreSQL 18 | Relational data, integer minor units for money (cents) and durations (minutes) |
| Migrations | Flyway | Schema in version control, reproducible DB |
| Dev runtime | Docker Compose | One command to start everything, hot reload in frontend |

## Structure

```
frontend/   React SPA (Vite dev server, proxies /api → backend)
backend/    Spring Boot, single module
  api/        OpenAPI config, exception handler
  project/    Project entity, repo, service, controller
  tracking/   TimeEntry entity, repo, service, controller
compose.yml + .env   All services + single source of truth for config
```

## Data model (V1)

- **projects** — name, description, time budget (minutes), cost budget (euro cents), hourly rate (euro cents/h)
- **time_entries** — date, FK → project, duration (minutes), optional description; `ON DELETE CASCADE` — deleting a project removes its entries
- Money and durations are stored as **integers in minor units** (cents / minutes) — no floating point, no rounding ambiguity anywhere in the stack

Both tables have a `version` column for optimistic locking.

## API design

Two resources, plain REST:

- `/api/projects` — CRUD, reads include computed budget status
- `/api/time-entries` — CRUD; `GET` filters are optional and combinable: `date` and/or `projectId` (no filter = all entries, no implicit "today" default)
- Optimistic locking is exposed: responses carry `version`, `PUT` requires it as `If-Match`; mismatch → 409

Budget logic lives in the **backend**, computed on read: ratio = recorded / budget, rounded to 4 decimals. `warning = ratio ≥ 0.80 && < 1.0`, `exceeded = ratio ≥ 1.0`. Cost = round(recordedMinutes / 60 × hourlyRateCents), HALF_UP.
## Validation rules

Enforced in the backend, mirrored client-side:

- Project: name required (≤100), budgets/rate > 0
- Time entry: date required, not in the future; duration > 0 and ≤ 1440 min; projectId must exist
- **Daily cap**: sum of all entries for a date + new duration ≤ 1440 min → else 422 (single entry > 1440 min is also 422)
- Errors return `{status, error, message, details[]}` — 400 validation, 404 not found, 422 business rule

## Decisions & trade-offs

- **No users** — single-user tool for now
- **No realtime tracking** — entries are created/edited manually via modal; simpler, matches the requirement.
- **`.env` as single source of truth** — compose substitutes `${POSTGRES_USER}` etc. everywhere;
- **Frontend polls nothing** — pages fetch on load and after mutations. Realtime is out of scope.
- **Hot reload in Docker** — source bind-mounted into the container
