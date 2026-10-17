# Next steps

6 hours go fast. I spent most of them on the two "must" areas and on tests.

## What is not in there yet

### Users and per-user hourly rates

Migration would be small: a `users(id, name)` table and a `user_id` column on `time_entries`, not null, foreign key. 
Add a non-null user_id to time_entries and backfill existing entries to a default user.

UI gets a user picker in the header; the selected id goes into a context and is sent on every request.

Two things in the backend change as a consequence:
- The 24 h rule becomes per user *and* per date. `totalMinutesForDate` and `totalMinutesForDateExcluding` both grow a `userId` parameter. 
- Cost stops being `recordedMinutes / 60 × project.hourlyRateCents`. With per-user rates the cost of a project is a sum over its entries, each at the rate that applies to that user. So `BudgetCalculator.recordedCostCents` as it stands would go away and the cost would come out of SQL e.g. `SUM(duration_minutes * COALESCE(override, project_rate))` joined against a `user_project_rates(user_id, project_id, hourly_rate_cents)` table.

Cost is derived, so changing an hourly rate retroactively rewrites history. Fine for a demo, wrong once invoices hang off it. The fix would be adding `valid_from` on the rate rows or persisting the applied rate on the entry.


### Weekly and monthly overview

2 read-only endpoints:

- `GET /api/reports/weekly?week=2026-W42` → minutes per day and per project
- `GET /api/reports/monthly?month=2026-10` → minutes and cost per project

On the frontend, two more pages with plain tables. 

### Filtering, sorting and pagination

The time entries endpoint currently filters by an exact `date` and/or `projectId` and returns everything that matches. 

`from`/`to` range instead of a single date, 
`Pageable` with a stable sort (`date DESC, id DESC`). 


### Deleting projects

`time_entries.project_id` has `ON DELETE CASCADE`,  deleting a project deletes every hour ever booked against it.

I would add an `archived` flag, exclude archived projects from the picker, keep  visible in reports.

### Concurrency on the daily cap

`TimeEntryService.create` reads the total for a date, then checks the cap in Java. Two requests at the same time both read 1380 min, both decide another 60 is fine, both commit; 25 h in the database.

The fix would be to serialize writes for a (user, date) key inside the transaction, e.g. using a PostgreSQL advisory lock or a locking row, and then perform the sum and insert atomically.

### Error format

The error responses are a custom but consistent shape. The brief mentions RFC 9457. I would switch, it is a breaking change for the frontend error handling, which is why it did not happen today.

### Tests

- Concurrency test for the cap above: two parallel inserts, assert one gets a 422.
- Every Flyway migration applies cleanly from an empty database.
- Accessibility: `@axe-core/playwright` on both main flows at both viewports.
- Error-path coverage in the frontend tests 

### Operational 

1. **CI.** No pipeline at all right now. 
2. **Authentication.** Spring Security resource server with OIDC/JWT, entries scoped to the token subject, and a role for managing projects. 
3. **Health and metrics.** Prometheus, real Docker healthcheck, structured logs with a request id.

## Would I split this into microservices?

No, and I would push back.

Domain is small, data tightly relational. Budget status is aggregate over time entries, validation needs consistent view over every entry and person.

`project` and `tracking` only meet through `ProjectRepository`. If a split is needed the seam is there.

If it did grow (more teams, invoicing, realtime tracking), the boundaries:

 - **Projects** — projects, budgets, rates. Barely any writes, read constantly by the others.
 - **Time tracking** — entries, the daily-hours rule. All the write volume, and the rule has to stay strongly consistent, so it owns it.
 - **Reporting** — read models only. Different load profile.

Communication: each owns its schema, no shared database. Time tracking keeps `projectId` as a plain reference and validates against a local replica of project ids fed by `ProjectUpdated` events. REST for commands that need an immediate answer, events over a broker.

I would split a service from the monolith if e.g. reporting slows time tracking.

## What would go in a NoSQL store?

PostgreSQL source of truth. Foreign keys, check constraints, the transactional
daily-total enforcement; without me implementing them in application code.

Where a document store could earn its place is as a derived read model. A monthly timesheet is an obvious one: 

```json
{
  "_id": "2026-10#user-1",
  "userId": "user-1",
  "month": "2026-10",
  "totalMinutes": 8550,
  "days": [
    { "date": "2026-10-05", "minutes": 480,
      "entries": [ { "entryId": 101, "projectId": 7, "projectName": "Website", "minutes": 480 } ] }
  ],
  "byProject": [ { "projectId": 7, "projectName": "Website", "minutes": 5760, "costCents": 480000 } ],
  "builtAt": "2026-11-01T00:05:00Z"
}
```

Partition by `userId`, sort by `month`. one document per user per month.
An append-only audit log of entry changes would fit too.

I would not move the write path, budgets or the daily limit check. And I would not add a second store until something is measurably slow. Index or a materialised view is a cheaper first answer.