package com.qavion.timetracking;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Base for REST tests: full Spring context, real PostgreSQL (Testcontainers), empty tables for every test. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresContainerConfig.class)
public abstract class ApiIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void cleanDatabase() {
        jdbc.execute("TRUNCATE time_entries, projects RESTART IDENTITY CASCADE");
    }

    protected ResultActions postJson(String url, String body) throws Exception {
        return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    protected ResultActions putJson(String url, String body) throws Exception {
        return mockMvc.perform(put(url).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    protected ResultActions getJson(String url) throws Exception {
        return mockMvc.perform(get(url));
    }

    protected ResultActions deleteJson(String url) throws Exception {
        return mockMvc.perform(delete(url));
    }

    protected static String projectJson(String name, String timeBudget, String costBudget, String rate) {
        return """
                {"name":"%s","timeBudgetHours":%s,"costBudgetEur":%s,"hourlyRateEur":%s}
                """.formatted(name, timeBudget, costBudget, rate);
    }

    protected static String entryJson(long projectId, String date, String hours) {
        return """
                {"projectId":%d,"entryDate":"%s","durationHours":%s,"description":"work"}
                """.formatted(projectId, date, hours);
    }

    protected long createProject(String name, String timeBudget, String costBudget, String rate) throws Exception {
        String body = postJson("/api/projects", projectJson(name, timeBudget, costBudget, rate))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    protected long createEntry(long projectId, String date, String hours) throws Exception {
        String body = postJson("/api/time-entries", entryJson(projectId, date, hours))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }
}
