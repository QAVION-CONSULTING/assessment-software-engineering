package com.qavion.timetracking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
@Import(PostgresContainerConfig.class)
class SchemaMigrationSmokeTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void flywayMigrationCreatesTables() {
        Integer tables = jdbc.queryForObject("""
                SELECT count(*) FROM information_schema.tables
                WHERE table_schema = 'public' AND table_name IN ('projects', 'time_entries')
                """, Integer.class);
        assertThat(tables).isEqualTo(2);
    }

    @Test
    void foreignKeyRejectsEntryForUnknownProject() {
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO time_entries (project_id, entry_date, duration_hours) VALUES (999999, CURRENT_DATE, 1.5)"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
