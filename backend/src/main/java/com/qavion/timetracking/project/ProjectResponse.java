package com.qavion.timetracking.project;

import java.math.BigDecimal;

public record ProjectResponse(
        Long id,
        String name,
        BigDecimal timeBudgetHours,
        BigDecimal costBudgetEur,
        BigDecimal hourlyRateEur) {

    static ProjectResponse from(Project project) {
        return new ProjectResponse(project.getId(), project.getName(), project.getTimeBudgetHours(),
                project.getCostBudgetEur(), project.getHourlyRateEur());
    }
}
