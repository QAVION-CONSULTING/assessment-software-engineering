package com.qavion.timetracking.project;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Limits mirror the NUMERIC/VARCHAR precision of the projects table. */
public record ProjectRequest(
        @NotBlank @Size(max = 255) String name,
        @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 8, fraction = 2) BigDecimal timeBudgetHours,
        @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 10, fraction = 2) BigDecimal costBudgetEur,
        @NotNull @DecimalMin("0") @Digits(integer = 8, fraction = 2) BigDecimal hourlyRateEur) {
}
