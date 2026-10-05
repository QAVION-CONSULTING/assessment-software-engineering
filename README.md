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
