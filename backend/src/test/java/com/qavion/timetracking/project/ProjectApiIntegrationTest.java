package com.qavion.timetracking.project;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.qavion.timetracking.ApiIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ProjectApiIntegrationTest extends ApiIntegrationTest {

    @Test
    void createReturns201WithLocationAndEmptyBudget() throws Exception {
        postJson("/api/projects", projectJson("Website", "100", "5000.50", "50"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/api/projects/\\d+")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Website"))
                .andExpect(jsonPath("$.costBudgetEur").value(5000.50))
                .andExpect(jsonPath("$.budget.totalHours").value(0))
                .andExpect(jsonPath("$.budget.overallStatus").value("OK"));
    }

    @Test
    void getReturnsCreatedProject() throws Exception {
        long id = createProject("Mobile App", "200", "10000", "75");

        getJson("/api/projects/" + id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Mobile App"))
                .andExpect(jsonPath("$.hourlyRateEur").value(75));
    }

    @Test
    void listContainsCreatedProjects() throws Exception {
        long first = createProject("Alpha", "10", "100", "1");
        long second = createProject("Beta", "10", "100", "1");

        getJson("/api/projects")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem((int) first)))
                .andExpect(jsonPath("$[*].id", hasItem((int) second)));
    }

    @Test
    void updateChangesProject() throws Exception {
        long id = createProject("Old", "10", "100", "1");

        putJson("/api/projects/" + id, projectJson("New", "20", "300", "15.25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New"))
                .andExpect(jsonPath("$.timeBudgetHours").value(20))
                .andExpect(jsonPath("$.hourlyRateEur").value(15.25));

        getJson("/api/projects/" + id).andExpect(jsonPath("$.name").value("New"));
    }

    @Test
    void zeroHourlyRateIsAllowed() throws Exception {
        postJson("/api/projects", projectJson("Pro bono", "10", "100", "0")).andExpect(status().isCreated());
    }

    @Test
    void invalidPayloadReturns400ProblemDetailsWithFieldErrors() throws Exception {
        postJson("/api/projects", projectJson("  ", "0", "-5", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("application/problem+json")))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.timeBudgetHours").exists())
                .andExpect(jsonPath("$.errors.costBudgetEur").exists())
                .andExpect(jsonPath("$.errors.hourlyRateEur").exists())
                .andExpect(jsonPath("$", not(hasKey("trace"))));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"name\":\"X\",\"timeBudgetHours\":10.123,\"costBudgetEur\":100,\"hourlyRateEur\":1}",
            "{\"name\":\"X\",\"timeBudgetHours\":10,\"costBudgetEur\":100,\"hourlyRateEur\":1.001}",
            "{\"name\":\"X\",\"timeBudgetHours\":123456789,\"costBudgetEur\":100,\"hourlyRateEur\":1}",
            "{\"timeBudgetHours\":10,\"costBudgetEur\":100,\"hourlyRateEur\":1}"
    })
    void payloadViolatingPrecisionOrMissingFieldsIsRejected(String body) throws Exception {
        postJson("/api/projects", body).andExpect(status().isBadRequest());
    }

    @Test
    void nameLongerThanColumnIsRejected() throws Exception {
        postJson("/api/projects", projectJson("x".repeat(256), "10", "100", "1")).andExpect(status().isBadRequest());
    }

    @Test
    void malformedJsonReturns400ProblemDetails() throws Exception {
        postJson("/api/projects", "{not json")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void unknownProjectReturns404ProblemDetails() throws Exception {
        getJson("/api/projects/424242")
                .andExpect(status().isNotFound())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("application/problem+json")))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Resource not found"))
                .andExpect(jsonPath("$.detail").value("Project with id 424242 not found."));

        putJson("/api/projects/424242", projectJson("X", "1", "1", "1")).andExpect(status().isNotFound());
    }

    @Test
    void nonNumericIdReturns400() throws Exception {
        getJson("/api/projects/abc").andExpect(status().isBadRequest());
    }
}
