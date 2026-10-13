package com.qavion.timetracking.timeentry;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TimeEntryResponse(
        Long id,
        Long projectId,
        LocalDate entryDate,
        BigDecimal durationHours,
        String description) {

    static TimeEntryResponse from(TimeEntry entry) {
        return new TimeEntryResponse(entry.getId(), entry.getProjectId(), entry.getEntryDate(),
                entry.getDurationHours(), entry.getDescription());
    }
}
