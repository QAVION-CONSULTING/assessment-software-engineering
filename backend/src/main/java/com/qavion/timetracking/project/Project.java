package com.qavion.timetracking.project;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private BigDecimal timeBudgetHours;
    private BigDecimal costBudgetEur;
    private BigDecimal hourlyRateEur;

    protected Project() {
    }

    public Project(String name, BigDecimal timeBudgetHours, BigDecimal costBudgetEur, BigDecimal hourlyRateEur) {
        update(name, timeBudgetHours, costBudgetEur, hourlyRateEur);
    }

    public void update(String name, BigDecimal timeBudgetHours, BigDecimal costBudgetEur, BigDecimal hourlyRateEur) {
        this.name = name;
        this.timeBudgetHours = timeBudgetHours;
        this.costBudgetEur = costBudgetEur;
        this.hourlyRateEur = hourlyRateEur;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getTimeBudgetHours() {
        return timeBudgetHours;
    }

    public BigDecimal getCostBudgetEur() {
        return costBudgetEur;
    }

    public BigDecimal getHourlyRateEur() {
        return hourlyRateEur;
    }
}
