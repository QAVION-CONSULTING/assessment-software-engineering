package com.example.timetracker.project;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A project with its time budget (minutes), cost budget (euro cents) and
 * hourly rate (euro cents per hour). Money and durations are stored as
 * integers in minor units — no floating point, no rounding ambiguity.
 */
@Entity
@Table(name = "projects")
@Getter
@Setter(AccessLevel.PRIVATE)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED) // for JPA
public class Project {

    public void rename(String newName) {
        this.name = newName;
    }

    public void changeDescription(String newDescription) {
        this.description = newDescription;
    }

    /**
     * Replaces the budgets and rate; the validator annotations on the
     * fields catch any invalid values at flush time.
     */
    public void replaceBudgets(Integer newTimeBudgetMinutes,
                               Long newCostBudgetCents,
                               Integer newHourlyRateCents) {
        this.timeBudgetMinutes = newTimeBudgetMinutes;
        this.costBudgetCents = newCostBudgetCents;
        this.hourlyRateCents = newHourlyRateCents;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String name;

    @Size(max = 500)
    @Column(length = 500)
    private String description; // optional

    /**
     * Time budget in minutes (7200 = 120 h).
     */
    @NotNull
    @Min(value = 1)
    @Column(name = "time_budget_minutes", nullable = false)
    private Integer timeBudgetMinutes;

    /**
     * Cost budget in euro cents (960000 = 9600.00 EUR).
     */
    @NotNull
    @Min(value = 1)
    @Column(name = "cost_budget_cents", nullable = false)
    private Long costBudgetCents;

    /**
     * Hourly rate in euro cents per hour (8000 = 80.00 EUR/h).
     */
    @NotNull
    @Min(value = 1)
    @Column(name = "hourly_rate_cents", nullable = false)
    private Integer hourlyRateCents;

    /**
     * Optimistic-locking version, exposed via the If-Match header.
     */
    @Version
    @Column(nullable = false)
    private Long version;

}