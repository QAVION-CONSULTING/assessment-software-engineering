# Time Tracker

Record working hours per project, keep an eye on budgets, get a warning when they run out.

React 19 + TypeScript SPA, Spring Boot (Java 21) REST API, PostgreSQL, Flyway migrations. Everything runs with Docker Compose.

## Prerequisites

- Docker with Docker Compose
- GNU Make (optional; the commands can also be run directly, see the `Makefile`)

## Quick start

```bash
cp example.env .env          # adjust ports/passwords if you like
docker compose up -d 
```

The first start builds the images and initializes the database. Then open http://localhost:8081 (or whatever `FRONTEND_PORT` you set).

- **Projects** — create projects with a time budget, cost budget and hourly rate. The list shows recorded hours/costs against the budgets with a progress bar, and a badge that flips to yellow at ≥ 80 % and red when exceeded.
- **Time Entries** — record hours per day against a project, filter by date/project, edit and delete. 

## Development

For backend + frontend with hot reload:

```bash
docker compose -f compose.yml -f compose.dev.yml up -d --build
```

- Frontend (Vite dev server): http://localhost:5173
- Backend API: http://localhost:8080/api

The frontend source is bind-mounted into the container. Backend changes need a rebuild (`docker compose up -d --build backend`).

The API contract lives in [`docs/openapi.yaml`](docs/openapi.yaml) — it is the single source of truth. The backend generates its API interfaces from it at build time, and the frontend generates a typed client:

```bash
cd frontend && npm run generate:api
```

## Testing

One command per layer (or `make test` for all three):

```bash
make test-backend    # JUnit 5 + Mockito + AssertJ; integration tests via Testcontainers 
make test-frontend   # Vitest + React Testing Library
make test-e2e       # Playwright against an isolated stack
```

Details on what is tested and why: [`docs/TESTING.md`](docs/TESTING.md)

## Docs

- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — stack, structure, data model, API design, decisions & trade-offs
- [`docs/TESTING.md`](docs/TESTING.md) — test strategy per layer
- [`docs/openapi.yaml`](docs/openapi.yaml) — the API contract
- [`docs/schema.sql`](docs/schema.sql) — the V1 schema as plain SQL

## Project layout

```
backend/     Spring Boot — api/, project/, tracking/ packages
frontend/    React SPA — pages/, api/ (generated typed client), e2e/
docs/        architecture, testing strategy, OpenAPI spec, schema
compose.yml  prod stack: frontend (nginx) + backend + postgres
compose.dev.yml   dev overlay: hot-reload Vite server
compose.e2e.yml   isolated stack for E2E tests
Makefile     test-backend / test-frontend / test-e2e / test
```