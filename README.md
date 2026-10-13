# Assessment for Software Engineering (Fullstack)

## Objective

Build a browser-based **time tracking application** that lets users assign their daily working hours to projects and keeps an eye on project budgets.

The solution consists of a frontend, a backend and a database. We are less interested in the number of features than in **how** you build them: architecture, data modelling, code quality, and above all **automated tests**.

## Process

1. **Take-home challenge** (this repository, max. 6 hours)
2. **Interview call**: you walk us through your solution and design decisions
3. **Live coding**: together we implement a small change in *your* solution

Because the live coding happens in your code, make sure you know every part of it well.

## Submission

- Create a feature branch from this repository.
- Work on a feature branch and open a pull request against `main` when you are done.
- Commit in small, meaningful steps. The history is part of the evaluation.
- AI coding assistants are allowed. You are responsible for every line and must be able to explain and change it live.

## Technical Requirements

### Frontend
- Single Page Application written in **TypeScript**
- Angular, React or Vue.js
- CSS framework of your choice (e.g. Tailwind CSS); additional UI libraries are allowed
- **Responsive**: usable on a desktop and on a mobile screen (~375 px wide)

### Backend
- RESTful API in **Java** with **Spring Boot** or **Quarkus**
- Input validation and consistent error responses (e.g. RFC 9457 Problem Details)
- API documentation (e.g. OpenAPI / Swagger UI)

### Database
- **Relational database**: PostgreSQL or MySQL (H2 is allowed for tests only)
- Schema managed with migrations (Flyway or Liquibase)
- Explain your data model briefly (an ER diagram or a short description is enough)

### Tests (mandatory)
- **Unit tests** for backend business logic (JUnit 5, Mockito, AssertJ)
- **Integration tests** for the REST API against a real database (e.g. Testcontainers)
- **Frontend tests** for at least one component or service
- **At least one end-to-end UI test** covering the main flow (Playwright, Cypress or Selenium)
- Tests run with a single command per layer (documented in your README)

### Running the application
- `docker compose up` starts frontend, backend and database
- Your README explains how to start, test and use the application

## Functional Requirements

### Projects and budgets (must)
- Create and edit projects with a **time budget** (hours), a **cost budget** (EUR) and an **hourly rate**
- Costs are calculated automatically from the recorded hours
- The UI shows a **warning** when a project reaches 80 % of its budget and when it exceeds it

### Daily time tracking (must)
- Record working time per day: **date, project, duration in hours** (all required), optional description
- Edit and delete entries
- Sensible validation (e.g. no negative durations, no more than 24 h per day)

### Weekly and monthly overview (should)
- Weekly view: total hours per day and per project
- Monthly view: total hours and costs per project

### Users and individual rates (should)
- Multiple users (a simple user selection is enough, no authentication required)
- Hourly rates can be set per user and override the project rate

## Time Frame

Please do not spend more than **6 hours**. Prioritise the "must" requirements and the tests over extra features.

Add a `NEXT_STEPS.md` with everything you did not implement and how you would approach it. Please include at least:

- How would you split this application into **microservices**, if at all? What would the service boundaries be and how would the services communicate?
- Which parts of the data would you store in a **NoSQL database** (e.g. MongoDB, DynamoDB, Cosmos DB), and what would that model look like?

## Evaluation Criteria

- Correctness and completeness of the "must" requirements
- Test strategy and test quality across all layers
- Architecture, code structure and readability (backend and frontend)
- Data model and migrations
- API design, validation and error handling
- Responsive, usable UI
- Commit history, documentation and how easy it is to run the project
- Quality of the reasoning in `NEXT_STEPS.md`

---

# Solution documentation

## Application overview

A browser-based time tracking application. Implemented:

- **Projects**: create and edit projects with a time budget (hours), a cost budget (EUR) and an hourly rate (EUR).
- **Time entries**: record date, project, duration (decimal hours) and an optional description; edit and delete entries (deletion asks for confirmation).
- **Automatic costs and budget warnings**: recorded hours, cost and utilization are calculated by the backend. The UI shows `OK`, `WARNING` (from 80 %) and `EXCEEDED` (above 100 %), separately for time and cost plus an overall status.
- **Validation**: field-level validation on the client and server, a 24 h/day limit, consistent RFC 9457 error responses.

Not implemented (optional "should" requirements, see [NEXT_STEPS.md](NEXT_STEPS.md)): weekly/monthly overview, multiple users and per-user rates.

## Technology stack

| Layer | Technology |
|---|---|
| Frontend | Vue 3.5, TypeScript 5.9, Vite 7, Vue Router 4.6, plain CSS (no UI framework, no state library) |
| Backend | Java 21, Spring Boot 3.5.6 (Web, Data JPA, Validation, Actuator), Maven, springdoc-openapi 2.8.13 |
| Database | PostgreSQL 17, Flyway migrations (version managed by Spring Boot) |
| Tests | JUnit 5, Mockito, AssertJ, Testcontainers 1.21.4 (backend); Vitest 3.2 + Vue Test Utils 2.5 (frontend); Playwright 1.64 (E2E) |
| Infrastructure | Docker, Docker Compose |

## Architecture

A **modular monolith**: one Spring Boot application, one database, packaged feature-first.

```
backend/src/main/java/com/qavion/timetracking
├── project/     Project entity, repository, service, controller, DTOs, BudgetCalculator
├── timeentry/   TimeEntry entity, repository, service, controller, DTOs, 24 h rule
└── common/      Problem Details exception handler, 404/422 exceptions, OpenAPI config
frontend/src
├── api/         typed fetch wrappers (one `request()` helper, `ApiError`, error mapping)
├── components/  forms, project card, budget badge/bar
├── views/       ProjectsView, TimeEntriesView (own their state and API calls)
├── types/       interfaces mirroring the backend DTOs
└── router/, utils/
```

Controllers are thin (HTTP mapping only); business rules live in services and the pure `BudgetCalculator`. The frontend never recalculates budget rules, it renders what the API returns.

## Data model

```
projects 1 ────< time_entries
```

| Table | Columns |
|---|---|
| `projects` | `id` (identity PK), `name` VARCHAR(255) not blank, `time_budget_hours` NUMERIC(10,2) > 0, `cost_budget_eur` NUMERIC(12,2) > 0, `hourly_rate_eur` NUMERIC(10,2) >= 0 |
| `time_entries` | `id` (identity PK), `project_id` FK → `projects`, `entry_date` DATE, `duration_hours` NUMERIC(4,2) > 0 and <= 24, `description` VARCHAR(1000) null |

- Indexes: `time_entries(project_id)` (per-project totals) and `time_entries(entry_date)` (daily total, future reporting).
- Money and hours are `NUMERIC` / `BigDecimal`, never floating point.
- Costs and utilization are **derived, never stored**.
- The schema lives in `backend/src/main/resources/db/migration/V1__create_projects_and_time_entries.sql`; Hibernate only validates it (`ddl-auto: validate`).

## Getting started

Prerequisites: Docker with Compose (`docker compose`, or the standalone `docker-compose`).

```bash
docker compose up --build
```

| What | URL |
|---|---|
| Frontend | http://localhost:5173 |
| Backend API | http://localhost:8080/api (also reachable through the frontend at `http://localhost:5173/api`) |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| Health | http://localhost:8080/api/actuator/health |

Stop with `Ctrl+C` or `docker compose down` (add `-v` to also delete the database volume).

Configuration uses development-friendly defaults; copy `.env.example` to `.env` to override. Nothing secret is committed.

| Variable | Default | Used by |
|---|---|---|
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | `timetracking` | compose (db and backend) |
| `BACKEND_PORT`, `FRONTEND_PORT` | `8080`, `5173` | compose published ports |
| `DB_URL`, `DB_USER`, `DB_PASSWORD`, `SERVER_PORT` | localhost defaults | backend (compose sets `DB_URL` to the `db` service host) |

## Running tests

Each layer has a single command.

| Layer | Command | Notes |
|---|---|---|
| Backend unit + REST integration | `cd backend && mvn verify` | Integration tests use **Testcontainers** (real PostgreSQL), so **Docker must be running** |
| Frontend typecheck | `cd frontend && npm install && npm run typecheck` | |
| Frontend component/service tests | `cd frontend && npm test` | Vitest, API boundaries mocked |
| End-to-end (Playwright) | see below | Runs against the real stack, nothing mocked |

**E2E**: start the application first (`docker compose up --build -d`), install the browser once (`cd frontend && npx playwright install chromium`), then:

```bash
cd frontend
E2E_BASE_URL=http://localhost:5173 npm run test:e2e
```

The tests run on a desktop and a mobile (Pixel 5) profile. They create uniquely named projects and use random historic dates so runs do not interfere (projects cannot be deleted, so test projects remain in the dev database; `docker compose down -v` resets it).

**macOS with Colima**: Colima does not create the default `/var/run/docker.sock`, so Testcontainers cannot find Docker on its own and every integration test fails at context start-up ("Could not find a valid Docker environment"). `DOCKER_HOST` points the Docker client at the Colima socket, and `TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE` tells Testcontainers which socket path to mount into its helper containers (the path as seen inside the Colima VM). One-off form:

```bash
cd backend
DOCKER_HOST="unix://${HOME}/.colima/default/docker.sock" \
TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock \
mvn clean verify
```

or export both variables once per shell and run `mvn verify` as usual. Docker Desktop users need neither variable.

`pom.xml` pins `testcontainers.version` to 1.21.4 because the version managed by Spring Boot 3.5.6 (1.21.3) cannot talk to Docker 29.

Test inventory: 72 backend tests (37 unit, 33 REST integration, 2 schema smoke), 47 frontend tests, 6 Playwright runs (3 tests × desktop/mobile).

## Business rules

- **Max 24 h per day**: the sum of all entries on one date, across all projects, may not exceed 24 h. Violations return HTTP 422. When editing, the entry's own previous duration is excluded if the date is unchanged, so moving an entry to another date or project cannot bypass the check. The sum is a single aggregate query; concurrent writes for the same date are serialized with a PostgreSQL transaction-scoped advisory lock (covered by a concurrency test).
- **Costs**: `totalCost = totalHours × project hourly rate`, `utilization = used / budget × 100`, for time and cost separately.
- **Status**: `OK` below 80 %, `WARNING` from 80 % up to and including 100 % (exactly 100 % is reached, not exceeded), `EXCEEDED` above 100 %. The overall status is the most severe of time and cost. Statuses are decided on exact values; displayed percentages and money are rounded HALF_UP to 2 decimals (so 79.996 % is shown as 80.00 % but is still `OK`).
- **Hourly rate changes are retroactive**: cost is always computed from the project's *current* rate, because costs are derived rather than stored. Changing the rate re-prices all existing entries. (Freezing historical costs would need a rate stored per entry or a rate history, see NEXT_STEPS.md.)
- **Single user**: there is no authentication or user concept; the 24 h rule applies to the whole installation.
- **Errors**: 400 invalid payload (with per-field `errors`), 404 unknown resource, 422 business rule (daily limit, or unknown `projectId` inside a time-entry body), all as RFC 9457 `application/problem+json`; unexpected errors return a generic 500 without internals.

## Technical decisions and trade-offs

- **Modular monolith** over services: one deployable, one transaction boundary, trivial to explain and change.
- **`TimeEntry.projectId` is a plain column** (the foreign key is enforced by the database) instead of a JPA association: no lazy loading surprises, and project listing uses one grouped query for all totals (no N+1).
- **Package coupling**: `project` reads hour totals via `TimeEntryRepository`, and `timeentry` checks project existence via `ProjectRepository`. This small bidirectional dependency is accepted instead of adding an abstraction layer.
- **No Lombok / mapper / generic CRUD frameworks**: entities, records as DTOs and explicit mapping keep the code small.
- **Client validation is only input hygiene**; the backend is the source of truth for every rule.
- **Frontend**: no state library; each view owns its state. The frontend container serves the build with `vite preview` plus an `/api` proxy, which is enough for a demo but should be replaced by a real web server for production (see NEXT_STEPS.md).
- **No pagination** on list endpoints (data volume of a single-user assessment).
