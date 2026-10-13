package com.qavion.timetracking.timeentry;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "time_entries")
public class TimeEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Plain id instead of an association: avoids lazy-loading pitfalls; the FK is enforced by the database.
    private Long projectId;
    private LocalDate entryDate;
    private BigDecimal durationHours;
    private String description;

    protected TimeEntry() {
    }

    public TimeEntry(Long projectId, LocalDate entryDate, BigDecimal durationHours, String description) {
        update(projectId, entryDate, durationHours, description);
    }

    public void update(Long projectId, LocalDate entryDate, BigDecimal durationHours, String description) {
        this.projectId = projectId;
        this.entryDate = entryDate;
        this.durationHours = durationHours;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public LocalDate getEntryDate() {
        return entryDate;
    }

    public BigDecimal getDurationHours() {
        return durationHours;
    }

    public String getDescription() {
        return description;
    }
}
