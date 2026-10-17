# Testing Strategy

## Backend — Unit tests

### Budget calculation (`BudgetCalculator` / project status logic)


0%	No flag
79.99%	No flag
Exactly 80%	Warning
99.99%	Warning
Exactly 100%	Exceeded
Above 100%	Exceeded

- time-budget and cost-budget flags are independent

### Daily-cap rule

Sum of entries for a date + new duration ≤ 1440 min, as a pure decision
(repo query mocked):

- boundary: exactly 1440 total → allowed; 1441 → rejected
- editing an entry excludes its own old duration from the sum
- entries across different projects on the same date share the cap

---

## Backend — Integration tests

### Happy paths 

- POST project → 201 with computed status; POST time entry → 201
- GET project / entry by id → 200; GET list with `date` + `projectId` filters
  combined → only matching rows
- PUT with correct `If-Match` → 200, version incremented
- DELETE → 204, subsequent GET → 404

### Error contract 
- one 400 (invalid body) — asserts shape `{status, error, message, details[]}`
- one 404 (unknown id)
- one 409 (stale `If-Match` on PUT)
- one 422 (business rule) — future date or unknown projectId surfacing as a
  clean error, not a 500

### Behaviors 
- **Daily cap end-to-end**: seed 1400 min for a date, POST 41 → 422, POST 40 → 201
- **Cascade delete**: deleting a project removes its time entries
- **Budget status from real data**: seed entries, assert computed ratio/cost in
  the project response (proves repo query + calculation are wired together)
- **Migrations**: Flyway V1 applies on a fresh container (implicit in every test)



## Frontend tests

Stack: **Vitest + React Testing Library**, run with `npm test` in `frontend/`.
The API layer (`src/api/endpoints.ts`) is mocked with `vi.mock`

### Unit — pure helpers

- `fmtEur` / `fmtHours` — cents→EUR, minutes→hours (de-DE locale)


### Component — `ProjectsPage`

- **create flow**: valid submit calls `createProject` with converted payloads
  (hours→minutes, EUR→cents) and reloads the list
- **error surfaces**: `ApiError` shows message + `details[]` in the modal
- **validation**: empty name / zero budgets → field errors, no API call

### Component — `TimeEntriesPage`

- **filters drive the query**: date/project filter → matching query params;
  clearing drops them
- **validation**: duration ≤ 0 or > 24 h → field error, no API call


## End-to-end UI tests 

Stack: **Playwright**, run with `npm run e2e` in `frontend/`

### Flow 

1. `/projects` → create a project → row appears with green "ok" badge
2. `/time-entries` → create an entry for it (today, 2 h) → row appears
3. back on `/projects` → time usage shows `2 h / budget`, progress bar updated
