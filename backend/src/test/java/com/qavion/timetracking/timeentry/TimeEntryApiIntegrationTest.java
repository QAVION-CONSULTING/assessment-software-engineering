package com.qavion.timetracking.timeentry;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.qavion.timetracking.ApiIntegrationTest;
import org.junit.jupiter.api.Test;

class TimeEntryApiIntegrationTest extends ApiIntegrationTest {

    // ---- CRUD ----

    @Test
    void createReturns201AndEntryCanBeRead() throws Exception {
        long project = createProject("P", "100", "1000", "10");

        postJson("/api/time-entries", entryJson(project, "2025-03-10", "7.5"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern("/api/time-entries/\\d+")))
                .andExpect(jsonPath("$.projectId").value(project))
                .andExpect(jsonPath("$.entryDate").value("2025-03-10"))
                .andExpect(jsonPath("$.durationHours").value(7.5))
                .andExpect(jsonPath("$.description").value("work"));

        long id = createEntry(project, "2025-03-11", "1");
        getJson("/api/time-entries/" + id).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
    }

    @Test
    void descriptionIsOptional() throws Exception {
        long project = createProject("P", "100", "1000", "10");

        postJson("/api/time-entries", "{\"projectId\":%d,\"entryDate\":\"2025-03-10\",\"durationHours\":2}".formatted(project))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").doesNotExist());
    }

    @Test
    void updateChangesEntry() throws Exception {
        long project = createProject("P", "100", "1000", "10");
        long other = createProject("Q", "100", "1000", "10");
        long id = createEntry(project, "2025-03-10", "2");

        putJson("/api/time-entries/" + id, entryJson(other, "2025-03-12", "3.25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(other))
                .andExpect(jsonPath("$.entryDate").value("2025-03-12"))
                .andExpect(jsonPath("$.durationHours").value(3.25));

        getJson("/api/time-entries/" + id).andExpect(jsonPath("$.projectId").value(other));
    }

    @Test
    void deleteReturns204AndEntryIsGone() throws Exception {
        long project = createProject("P", "100", "1000", "10");
        long id = createEntry(project, "2025-03-10", "2");

        deleteJson("/api/time-entries/" + id).andExpect(status().isNoContent());

        getJson("/api/time-entries/" + id).andExpect(status().isNotFound());
        deleteJson("/api/time-entries/" + id).andExpect(status().isNotFound());
    }

    @Test
    void listSupportsProjectAndDateFilters() throws Exception {
        long a = createProject("A", "100", "1000", "10");
        long b = createProject("B", "100", "1000", "10");
        long a1 = createEntry(a, "2025-03-03", "1");
        long a2 = createEntry(a, "2025-03-10", "1");
        long b1 = createEntry(b, "2025-03-10", "1");

        getJson("/api/time-entries").andExpect(jsonPath("$", hasSize(3)));

        getJson("/api/time-entries?projectId=" + a)
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].id", hasItem((int) a1)))
                .andExpect(jsonPath("$[*].id", hasItem((int) a2)))
                .andExpect(jsonPath("$[*].id", not(hasItem((int) b1))));

        getJson("/api/time-entries?from=2025-03-10&to=2025-03-10")
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].id", hasItem((int) a2)))
                .andExpect(jsonPath("$[*].id", hasItem((int) b1)));

        getJson("/api/time-entries?projectId=" + b + "&from=2025-03-04")
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(b1));
    }

    // ---- validation and errors ----

    @Test
    void invalidPayloadReturns400WithFieldErrors() throws Exception {
        postJson("/api/time-entries", "{\"durationHours\":25,\"description\":\"" + "x".repeat(1001) + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                .andExpect(jsonPath("$.errors.projectId").exists())
                .andExpect(jsonPath("$.errors.entryDate").exists())
                .andExpect(jsonPath("$.errors.durationHours").exists())
                .andExpect(jsonPath("$.errors.description").exists());
    }

    @Test
    void zeroNegativeAndTooPreciseDurationsAreRejected() throws Exception {
        long project = createProject("P", "100", "1000", "10");

        for (String hours : new String[] {"0", "-1", "24.01", "1.234"}) {
            postJson("/api/time-entries", entryJson(project, "2025-03-10", hours))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.durationHours").exists());
        }
    }

    @Test
    void invalidDateFormatReturns400() throws Exception {
        long project = createProject("P", "100", "1000", "10");

        postJson("/api/time-entries", entryJson(project, "not-a-date", "1")).andExpect(status().isBadRequest());
    }

    @Test
    void unknownProjectReturns422ProblemDetails() throws Exception {
        postJson("/api/time-entries", entryJson(424242, "2025-03-10", "1"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.detail").value("Project with id 424242 does not exist."));
    }

    @Test
    void unknownEntryReturns404ForGetPutDelete() throws Exception {
        long project = createProject("P", "100", "1000", "10");

        getJson("/api/time-entries/424242")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource not found"));
        putJson("/api/time-entries/424242", entryJson(project, "2025-03-10", "1")).andExpect(status().isNotFound());
        deleteJson("/api/time-entries/424242").andExpect(status().isNotFound());
    }

    // ---- 24 h per day ----

    @Test
    void dailyLimitIsEnforcedAcrossProjects() throws Exception {
        long a = createProject("A", "100", "1000", "10");
        long b = createProject("B", "100", "1000", "10");
        createEntry(a, "2025-03-10", "15");
        createEntry(b, "2025-03-10", "8");

        postJson("/api/time-entries", entryJson(a, "2025-03-10", "2"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.title").value("Business rule violated"))
                .andExpect(jsonPath("$.detail", containsString("daily maximum is 24")));

        // exactly 24 h is fine, and another day is unaffected
        postJson("/api/time-entries", entryJson(a, "2025-03-10", "1")).andExpect(status().isCreated());
        postJson("/api/time-entries", entryJson(a, "2025-03-11", "10")).andExpect(status().isCreated());
    }

    @Test
    void editingEntryExcludesItsOwnHoursFromDailyTotal() throws Exception {
        long project = createProject("P", "100", "1000", "10");
        createEntry(project, "2025-03-10", "10");
        long id = createEntry(project, "2025-03-10", "10");

        putJson("/api/time-entries/" + id, entryJson(project, "2025-03-10", "14")).andExpect(status().isOk());
        putJson("/api/time-entries/" + id, entryJson(project, "2025-03-10", "14.5"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void movingEntryToFullDayIsRejected() throws Exception {
        long project = createProject("P", "100", "1000", "10");
        createEntry(project, "2025-03-10", "24");
        long id = createEntry(project, "2025-03-11", "1");

        putJson("/api/time-entries/" + id, entryJson(project, "2025-03-10", "1"))
                .andExpect(status().isUnprocessableEntity());
        getJson("/api/time-entries/" + id).andExpect(jsonPath("$.entryDate").value("2025-03-11"));
    }

    @Test
    void deletingEntryFreesDailyCapacity() throws Exception {
        long project = createProject("P", "100", "1000", "10");
        long id = createEntry(project, "2025-03-10", "24");
        postJson("/api/time-entries", entryJson(project, "2025-03-10", "1")).andExpect(status().isUnprocessableEntity());

        deleteJson("/api/time-entries/" + id).andExpect(status().isNoContent());

        postJson("/api/time-entries", entryJson(project, "2025-03-10", "1")).andExpect(status().isCreated());
    }

    @Test
    void concurrentRequestsCannotExceedDailyLimit() throws Exception {
        long project = createProject("P", "100", "1000", "10");
        int requests = 8; // 5 h each: at most 4 fit into 24 h
        var pool = java.util.concurrent.Executors.newFixedThreadPool(requests);
        try {
            var start = new java.util.concurrent.CountDownLatch(1);
            java.util.List<java.util.concurrent.Future<Integer>> results = new java.util.ArrayList<>();
            for (int i = 0; i < requests; i++) {
                results.add(pool.submit(() -> {
                    start.await();
                    return postJson("/api/time-entries", entryJson(project, "2025-03-10", "5"))
                            .andReturn().getResponse().getStatus();
                }));
            }
            start.countDown();
            int created = 0;
            for (var result : results) {
                if (result.get() == 201) {
                    created++;
                }
            }
            org.assertj.core.api.Assertions.assertThat(created).isEqualTo(4);
        } finally {
            pool.shutdownNow();
        }
        getJson("/api/time-entries?from=2025-03-10&to=2025-03-10").andExpect(jsonPath("$", hasSize(4)));
    }

    // ---- budget calculations ----

    @Test
    void projectBudgetFollowsCreatingUpdatingAndDeletingEntries() throws Exception {
        // 100 h / 10 000 EUR at 20 EUR/h -> time budget is the limiting one (cost is 20 % of it per hour)
        long project = createProject("P", "100", "10000", "20");

        getJson("/api/projects/" + project)
                .andExpect(jsonPath("$.budget.totalHours").value(0))
                .andExpect(jsonPath("$.budget.totalCostEur").value(0))
                .andExpect(jsonPath("$.budget.overallStatus").value("OK"));

        long first = createEntry(project, "2025-03-03", "8");
        createEntry(project, "2025-03-04", "7.5");
        createEntry(project, "2025-03-05", "8"); // 23.5 h so far
        getJson("/api/projects/" + project)
                .andExpect(jsonPath("$.budget.totalHours").value(23.5))
                .andExpect(jsonPath("$.budget.totalCostEur").value(470))
                .andExpect(jsonPath("$.budget.timeUtilizationPercent").value(23.5))
                .andExpect(jsonPath("$.budget.costUtilizationPercent").value(4.7))
                .andExpect(jsonPath("$.budget.overallStatus").value("OK"));

        // grow to 80 h -> WARNING
        for (String day : new String[] {"06", "07", "10", "11", "12", "13", "14"}) {
            createEntry(project, "2025-03-" + day, "8");
        }
        createEntry(project, "2025-03-17", "0.5");
        getJson("/api/projects/" + project)
                .andExpect(jsonPath("$.budget.totalHours").value(80))
                .andExpect(jsonPath("$.budget.timeStatus").value("WARNING"))
                .andExpect(jsonPath("$.budget.costStatus").value("OK"))
                .andExpect(jsonPath("$.budget.overallStatus").value("WARNING"));

        // updating an entry to push past 100 h -> EXCEEDED; reaching exactly 100 h is not exceeded
        putJson("/api/time-entries/" + first, entryJson(project, "2025-03-03", "20")).andExpect(status().isOk());
        getJson("/api/projects/" + project)
                .andExpect(jsonPath("$.budget.totalHours").value(92))
                .andExpect(jsonPath("$.budget.timeStatus").value("WARNING"));
        long extra = createEntry(project, "2025-03-18", "8");
        getJson("/api/projects/" + project).andExpect(jsonPath("$.budget.totalHours").value(100))
                .andExpect(jsonPath("$.budget.timeStatus").value("WARNING"));
        long over = createEntry(project, "2025-03-19", "0.25");
        getJson("/api/projects/" + project)
                .andExpect(jsonPath("$.budget.timeStatus").value("EXCEEDED"))
                .andExpect(jsonPath("$.budget.overallStatus").value("EXCEEDED"));

        // deleting brings it back down
        deleteJson("/api/time-entries/" + over).andExpect(status().isNoContent());
        deleteJson("/api/time-entries/" + extra).andExpect(status().isNoContent());
        getJson("/api/projects/" + project)
                .andExpect(jsonPath("$.budget.totalHours").value(92))
                .andExpect(jsonPath("$.budget.overallStatus").value("WARNING"));
    }

    @Test
    void costBudgetCanBeExceededWhileTimeBudgetIsFine() throws Exception {
        long project = createProject("Expensive", "1000", "1000", "100");
        createEntry(project, "2025-03-10", "10.01"); // 1001 EUR of 1000 EUR, 1 % of the time budget

        getJson("/api/projects/" + project)
                .andExpect(jsonPath("$.budget.timeStatus").value("OK"))
                .andExpect(jsonPath("$.budget.costStatus").value("EXCEEDED"))
                .andExpect(jsonPath("$.budget.overallStatus").value("EXCEEDED"))
                .andExpect(jsonPath("$.budget.totalCostEur").value(1001));
    }

    @Test
    void listCalculatesBudgetPerProject() throws Exception {
        long busy = createProject("Busy", "10", "1000", "1");
        long idle = createProject("Idle", "10", "1000", "1");
        createEntry(busy, "2025-03-10", "9");

        getJson("/api/projects")
                .andExpect(jsonPath("$[?(@.id==%d)].budget.timeStatus".formatted(busy)).value("WARNING"))
                .andExpect(jsonPath("$[?(@.id==%d)].budget.totalHours".formatted(idle)).value(0.0));
    }

    @Test
    void changingHourlyRateRecalculatesCostRetroactively() throws Exception {
        long project = createProject("P", "1000", "1000", "10");
        createEntry(project, "2025-03-10", "10"); // 100 EUR

        putJson("/api/projects/" + project, projectJson("P", "1000", "1000", "90"))
                .andExpect(jsonPath("$.budget.totalCostEur").value(900))
                .andExpect(jsonPath("$.budget.costStatus").value("WARNING"));
    }
}
