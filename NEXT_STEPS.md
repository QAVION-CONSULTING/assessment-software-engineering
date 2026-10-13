# Next steps

What was deliberately left out, and how I would approach it. Items are ordered by impact within each section.

## A. Unimplemented features

**Multiple users and user-specific hourly rates** (highest value, it unlocks most of the rest)
- Add `users(id, name)` and `time_entries.user_id NOT NULL`. A simple user picker in the UI stores the selected user id and sends it with requests (e.g. an `X-User-Id` header until real authentication exists).
- The 24 h rule becomes per user per date: the aggregate query and the advisory-lock key both gain the user id.
- Rates: `user_project_rates(user_id, project_id, hourly_rate_eur, valid_from)`. Effective rate = user override if present, otherwise the project rate. Cost is then `Σ(entry hours × effective rate)`, i.e. a `SUM` in SQL joined with the rates instead of `totalHours × rate`.
- Decide the historical-cost question at this point: either keep derived costs (current behaviour, rate changes are retroactive) or store the applied rate on each entry / use `valid_from` ranges so past costs stay stable.

**Weekly and monthly reporting**
- Read-only endpoints, no new tables: `GET /api/reports/weekly?week=2025-W11` returns hours per day and per project, `GET /api/reports/monthly?month=2025-03` returns hours and cost per project.
- Implement with grouped queries on `time_entries` (the `entry_date` index already exists) and reuse `BudgetCalculator`/the rate logic for costs. Date ranges are computed server-side (ISO weeks) to avoid time-zone drift. The UI adds two views with simple tables; no chart library is required.

**Filtering and pagination**
- Time entries already filter by `projectId`, `from`, `to`. Add `Pageable` (`page`, `size`, stable sort by date desc, id desc) returning a page DTO, and a project/date filter bar in the UI. Projects are few, so a search box over a paginated list is enough.

**Project deletion / archiving**
- Prefer an `archived` flag over hard deletes, since entries reference projects.

**Additional accessibility testing**
- Add `@axe-core/playwright` checks to the E2E flows (both viewports), keyboard-only test of the forms, and verify screen-reader announcements for status and error regions. Known gap: success/error live regions are inserted dynamically, which some screen readers announce unreliably; render persistent empty live regions instead.

## B. Microservices?

**Not now.** The domain is small, the data is strongly relational (entries reference projects, budgets are aggregates over entries), and the key business rule (24 h/day across all projects) needs a consistent view of all entries. In a monolith that is one transaction and one query. Splitting would replace it with distributed consistency, network failure modes, several deployables, contract testing and operational overhead, without a scaling or organizational reason to pay for it. The modular monolith keeps boundaries (`project`, `timeentry`) explicit so a split stays possible.

**If it grew** (many teams, very different load profiles, heavy reporting), reasonable boundaries would be:

| Service | Owns | Notes |
|---|---|---|
| Project management | projects, budgets, rates | Low write volume, read by others |
| Time tracking | time entries, daily-hours rule | Highest write volume; must be strongly consistent per user per day, so it owns that rule |
| Reporting | read models only | Scales independently, can lag slightly |

- **Data ownership**: each service owns its tables; no shared database. Time tracking stores `projectId` as a reference and validates it through the project service (sync REST call with a cached "project exists" check) or, more robustly, via a local replica of project ids fed by events.
- **Communication**: synchronous REST for commands that need an immediate answer (project lookup); asynchronous events (`ProjectUpdated`, `TimeEntryRecorded/Changed/Deleted`) over a broker (Kafka/RabbitMQ, with the outbox pattern) for reporting and budget alerts.
- **Consistency**: budget status becomes eventually consistent when computed from events; the daily-hours rule stays strongly consistent because it lives inside one service. Cross-service flows use idempotent consumers and compensation instead of distributed transactions.
- **Operational cost**: tracing, per-service CI/CD and deployments, schema/contract versioning, local development complexity. That cost is only justified by a real trigger, e.g. the reporting load hurting transactional latency (first split: Reporting) or separate teams owning projects and time tracking.

## C. NoSQL

**PostgreSQL stays the system of record.** The data is relational with integrity requirements (foreign keys, check constraints, transactional daily-total enforcement, decimal arithmetic), which a document store would push into application code.

Where a document database (MongoDB, DynamoDB, Cosmos DB) could add value as a **secondary, derived store**:

- **Denormalized reporting / timesheet read models**, rebuilt from events or from PostgreSQL. A monthly timesheet is read as a whole and never joined, which fits a document well:

```json
{
  "_id": "2025-03#user-1",
  "userId": "user-1",
  "month": "2025-03",
  "totalHours": 142.5,
  "days": [
    { "date": "2025-03-03", "hours": 8.0,
      "entries": [ { "entryId": 101, "projectId": 7, "projectName": "Website", "hours": 8.0 } ] }
  ],
  "byProject": [ { "projectId": 7, "projectName": "Website", "hours": 96.0, "costEur": "4800.00" } ],
  "builtAt": "2025-04-01T00:05:00Z"
}
```
  Keys: partition by `userId`, sort by `month` (DynamoDB) or compound `_id` (MongoDB). Money is stored as strings/Decimal128, never doubles.
- **Audit / event log** (who changed which entry, append-only, schema varies by event type) is a natural fit for a document or log store with a TTL/retention policy.
- **Not** for the transactional write path, budgets, or the daily-limit check.

Adding a second store is only worth it when reporting queries get expensive; before that, PostgreSQL indexes or a materialized view are simpler.

## D. Production improvements (by impact)

1. **Authentication and authorization**: required before multi-user. OIDC/JWT (e.g. Spring Security resource server), users may only see and edit their own entries, plus a role for managing projects. Includes CORS/CSRF decisions and rate limiting.
2. **Concurrency-safe daily-hour enforcement**: now done with a per-date PostgreSQL advisory lock inside the write transaction (covered by an integration test). Remaining work: include the user in the lock key, and consider a constraint-based alternative (e.g. a `daily_totals` row with a check constraint updated atomically) if the lock becomes a bottleneck.
3. **CI/CD**: pipeline running `mvn verify` (Docker service for Testcontainers), `npm run typecheck && npm test`, building both images, and Playwright against a compose stack on every PR; publish images and deploy on tag.
4. **Production frontend serving**: replace `vite preview` with an nginx (or CDN) image serving the static build with cache headers and the `/api` reverse proxy; run as non-root; add a healthcheck.
5. **Pagination** for time entries (and any report endpoints), with an index matching the default sort.
6. **Monitoring and observability**: Actuator metrics + Prometheus/Grafana, structured JSON logs with a correlation id, distributed tracing (OpenTelemetry), alerts on error rate and latency; expose health/metrics on a separate management port.
7. **Test-data isolation**: E2E tests currently use unique names and random dates against a shared dev database. Better: a dedicated compose profile with a throwaway database per run, or a test-only reset endpoint/profile; add project archiving so test data can be removed.
8. **Database migration strategy**: Flyway forward-only migrations, never edit applied ones; expand/contract for breaking changes (add column nullable → backfill → enforce); run migrations at deploy time with a backup and a tested rollback plan; keep Testcontainers tests running against every migration.
9. **Smaller hardening items**: pin the base images by digest, add dependency/vulnerability scanning, document backups for the Postgres volume.
