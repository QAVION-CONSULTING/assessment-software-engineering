package com.example.timetracker;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration tests for the REST API against a real PostgreSQL
 * (Testcontainers) — happy paths and the error contract, per
 * docs/TESTING.md.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
@DisplayName("Timetracker API (integration)")
class ApiIntegrationTest {

    @LocalServerPort
    int port;

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
    }

    // ---------- helpers ----------

    /** Creates a project and returns its id. */
    private int createProject(String name, int timeBudgetMinutes, int costBudgetCents, int hourlyRateCents) {
        return given().contentType(ContentType.JSON).body("""
                {
                  "name": "%s",
                  "timeBudgetMinutes": %d,
                  "costBudgetCents": %d,
                  "hourlyRateCents": %d
                }
                """.formatted(name, timeBudgetMinutes, costBudgetCents, hourlyRateCents))
                .when().post("/api/projects")
                .then().statusCode(201)
                .extract().jsonPath().getInt("id");
    }

    /** Creates a time entry; returns the full response for chaining. */
    private Response createTimeEntry(String date, int projectId, int durationMinutes) {
        return given().contentType(ContentType.JSON).body("""
                {
                  "date": "%s",
                  "projectId": %d,
                  "durationMinutes": %d
                }
                """.formatted(date, projectId, durationMinutes))
                .when().post("/api/time-entries");
    }

    // ---------- happy paths ----------

    @Nested
    @DisplayName("happy paths")
    class HappyPaths {

        @Test
        @DisplayName("POST project → 201 with computed status; POST entry → 201")
        void createProjectAndEntry() {
            int projectId = createProject("Happy Path", 600, 960000, 8000);

            given().get("/api/projects/{id}", projectId)
                    .then().statusCode(200)
                    .body("recordedMinutes", is(0))
                    .body("timeBudgetRatio", is(0.0f))
                    .body("timeBudgetWarning", is(false));

            createTimeEntry("2026-10-10", projectId, 450)
                    .then().statusCode(201)
                    .body("projectName", equalTo("Happy Path"))
                    .body("durationMinutes", is(450));
        }

        @Test
        @DisplayName("GET list with date + projectId filters combined → only matching rows")
        void listWithCombinedFilters() {
            int p1 = createProject("Filter P1", 600, 960000, 8000);
            int p2 = createProject("Filter P2", 600, 960000, 8000);

            // Unique date so other tests' data cannot interfere (the DB
            // is shared across the test class).
            String date = "2026-03-11";
            createTimeEntry(date, p1, 60).then().statusCode(201);
            createTimeEntry(date, p2, 60).then().statusCode(201);
            createTimeEntry("2026-03-12", p1, 60).then().statusCode(201);

            given().queryParam("date", date).queryParam("projectId", p1)
                    .get("/api/time-entries")
                    .then().statusCode(200)
                    .body("size()", is(1))
                    .body("[0].projectId", is(p1))
                    .body("[0].date", is(date));

            // date only → both entries on that date
            given().queryParam("date", date)
                    .get("/api/time-entries")
                    .then().statusCode(200)
                    .body("size()", is(2));

            // projectId only → all entries of that project
            given().queryParam("projectId", p1)
                    .get("/api/time-entries")
                    .then().statusCode(200)
                    .body("size()", is(2));
        }

        @Test
        @DisplayName("PUT with correct If-Match → 200, version incremented")
        void updateWithCorrectVersion() {
            int projectId = createProject("Versioned", 600, 960000, 8000);

            Response read = given().get("/api/projects/{id}", projectId)
                    .then().statusCode(200)
                    .extract().response();
            int version = read.jsonPath().getInt("version");

            given().contentType(ContentType.JSON)
                    .header("If-Match", version)
                    .body("""
                            {
                              "name": "Versioned v2",
                              "timeBudgetMinutes": 1200,
                              "costBudgetCents": 960000,
                              "hourlyRateCents": 8000
                            }
                            """)
                    .put("/api/projects/{id}", projectId)
                    .then().statusCode(200)
                    .body("name", equalTo("Versioned v2"))
                    .body("version", is(version + 1));
        }

        @Test
        @DisplayName("DELETE → 204, subsequent GET → 404")
        void deleteThenGet() {
            int projectId = createProject("Doomed", 600, 960000, 8000);

            given().delete("/api/projects/{id}", projectId)
                    .then().statusCode(204);

            given().get("/api/projects/{id}", projectId)
                    .then().statusCode(404);
        }
    }

    // ---------- error contract ----------

    @Nested
    @DisplayName("error contract")
    class ErrorContract {

        @Test
        @DisplayName("400 invalid body → {status, error, message, details[]}")
        void validationError() {
            given().contentType(ContentType.JSON).body("""
                    { "name": "", "timeBudgetMinutes": 0, "costBudgetCents": -5, "hourlyRateCents": 8000 }
                    """)
                    .post("/api/projects")
                    .then().statusCode(400)
                    .body("status", is(400))
                    .body("error", is("Bad Request"))
                    .body("message", is("Validation failed"))
                    .body("details", hasItems(
                            "name: size must be between 1 and 100",
                            "timeBudgetMinutes: must be greater than or equal to 1"));
        }

        @Test
        @DisplayName("404 unknown id")
        void notFound() {
            given().get("/api/projects/{id}", 999999)
                    .then().statusCode(404)
                    .body("status", is(404))
                    .body("error", is("Not Found"))
                    .body("message", is("Project not found: 999999"))
                    .body("details.size()", is(0));
        }

        @Test
        @DisplayName("409 stale If-Match on PUT")
        void versionConflict() {
            int projectId = createProject("Conflict", 600, 960000, 8000);

            given().contentType(ContentType.JSON)
                    .header("If-Match", 999) // never the current version
                    .body("""
                            {
                              "name": "Conflict v2",
                              "timeBudgetMinutes": 600,
                              "costBudgetCents": 960000,
                              "hourlyRateCents": 8000
                            }
                            """)
                    .put("/api/projects/{id}", projectId)
                    .then().statusCode(409)
                    .body("status", is(409))
                    .body("error", is("Conflict"))
                    .body("message", equalTo("Version mismatch: expected 999, current is 0"));
        }

        @Test
        @DisplayName("422 unknown projectId → clean error, not a 500")
        void unknownProjectIs422() {
            createTimeEntry("2026-10-10", 999999, 60)
                    .then().statusCode(422)
                    .body("status", is(422))
                    .body("error", is("Unprocessable Entity"))
                    .body("message", is("Project not found: 999999"));
        }

        @Test
        @DisplayName("422 future date → clean error, not a 500")
        void futureDateIs422() {
            int projectId = createProject("Future", 600, 960000, 8000);
            createTimeEntry("2099-01-01", projectId, 60)
                    .then().statusCode(400) // bean validation (@PastOrPresent) → 400
                    .body("status", is(400))
                    .body("details.size()", is(1));
        }
    }

    // ---------- behaviors ----------

    @Nested
    @DisplayName("behaviors")
    class Behaviors {

        @Test
        @DisplayName("daily cap end-to-end: 1400 seeded, POST 41 → 422, POST 40 → 201")
        void dailyCapEndToEnd() {
            int projectId = createProject("Cap", 6000, 960000, 8000);
            createTimeEntry("2026-10-14", projectId, 1400).then().statusCode(201);

            createTimeEntry("2026-10-14", projectId, 41)
                    .then().statusCode(422)
                    .body("message", equalTo(
                            "Total time for 2026-10-14 would exceed 24 h (already recorded: 23,33 h)"));

            createTimeEntry("2026-10-14", projectId, 40)
                    .then().statusCode(201);
        }

        @Test
        @DisplayName("cascade delete: deleting a project removes its time entries")
        void cascadeDelete() {
            int projectId = createProject("Cascade", 600, 960000, 8000);
            createTimeEntry("2026-10-15", projectId, 120).then().statusCode(201);

            given().delete("/api/projects/{id}", projectId).then().statusCode(204);

            Response remaining = given().queryParam("projectId", projectId)
                    .get("/api/time-entries")
                    .then().statusCode(200)
                    .extract().response();
            assertThat(remaining.jsonPath().<Integer>getList("")).isEmpty();
        }

        @Test
        @DisplayName("budget status from real data: ratio/cost computed from seeded entries")
        void budgetStatusFromRealData() {
            // 480 min = 8 h × 8000 cents/h = 64000 cents; budget 600 min → 0.8
            int projectId = createProject("Budget", 600, 960000, 8000);
            createTimeEntry("2026-10-16", projectId, 300).then().statusCode(201);
            createTimeEntry("2026-10-16", projectId, 180).then().statusCode(201);

            given().get("/api/projects/{id}", projectId)
                    .then().statusCode(200)
                    .body("recordedMinutes", is(480))
                    .body("recordedCostCents", is(64000))
                    .body("timeBudgetRatio", is(0.8f))
                    .body("costBudgetRatio", is(0.0667f))
                    .body("timeBudgetWarning", is(true))
                    .body("timeBudgetExceeded", is(false))
                    .body("costBudgetWarning", is(false))
                    .body("costBudgetExceeded", is(false));
        }
    }
}
