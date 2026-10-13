package com.qavion.timetracking.timeentry;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Limits mirror the time_entries table (NUMERIC(4,2), VARCHAR(1000)). */
public record TimeEntryRequest(
        @NotNull Long projectId,
        @NotNull LocalDate entryDate,
        @NotNull @DecimalMin(value = "0", inclusive = false) @DecimalMax("24") @Digits(integer = 2, fraction = 2) BigDecimal durationHours,
        @Size(max = 1000) String description) {
}
