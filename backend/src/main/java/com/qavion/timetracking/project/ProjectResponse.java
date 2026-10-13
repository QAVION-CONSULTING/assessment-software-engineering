package com.qavion.timetracking.project;

import java.math.BigDecimal;

public record ProjectResponse(
        Long id,
        String name,
        BigDecimal timeBudgetHours,
        BigDecimal costBudgetEur,
        BigDecimal hourlyRateEur,
        BudgetSummary budget) {

    static ProjectResponse from(Project project, BigDecimal totalHours) {
        return new ProjectResponse(project.getId(), project.getName(), project.getTimeBudgetHours(),
                project.getCostBudgetEur(), project.getHourlyRateEur(),
                BudgetCalculator.calculate(project, totalHours));
    }
}
