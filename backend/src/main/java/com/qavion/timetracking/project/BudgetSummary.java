package com.qavion.timetracking.project;

import java.math.BigDecimal;

/** Derived budget figures of a project; never persisted. Percentages are rounded to 2 decimals for display. */
public record BudgetSummary(
        BigDecimal totalHours,
        BigDecimal totalCostEur,
        BigDecimal timeUtilizationPercent,
        BigDecimal costUtilizationPercent,
        BudgetStatus timeStatus,
        BudgetStatus costStatus,
        BudgetStatus overallStatus) {
}
