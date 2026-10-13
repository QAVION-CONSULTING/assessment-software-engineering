package com.qavion.timetracking.project;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Pure budget arithmetic.
 * <ul>
 *   <li>Statuses are decided on exact (unrounded) values, so 79.996% is still OK, not WARNING.</li>
 *   <li>Displayed money and percentages are rounded HALF_UP to 2 decimals.</li>
 *   <li>Exactly 100% is WARNING (reached, not exceeded); only above 100% is EXCEEDED.</li>
 * </ul>
 */
public final class BudgetCalculator {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal WARNING_THRESHOLD_PERCENT = BigDecimal.valueOf(80);
    private static final int SCALE = 2;

    private BudgetCalculator() {
    }

    public static BudgetSummary calculate(Project project, BigDecimal totalHours) {
        BigDecimal exactCost = totalHours.multiply(project.getHourlyRateEur());

        BudgetStatus timeStatus = statusOf(totalHours, project.getTimeBudgetHours());
        BudgetStatus costStatus = statusOf(exactCost, project.getCostBudgetEur());

        return new BudgetSummary(
                totalHours.setScale(SCALE, RoundingMode.HALF_UP),
                exactCost.setScale(SCALE, RoundingMode.HALF_UP),
                percent(totalHours, project.getTimeBudgetHours()),
                percent(exactCost, project.getCostBudgetEur()),
                timeStatus,
                costStatus,
                BudgetStatus.mostSevere(timeStatus, costStatus));
    }

    /** Compares by cross-multiplication, avoiding division and rounding. Budgets are validated as positive. */
    private static BudgetStatus statusOf(BigDecimal used, BigDecimal budget) {
        if (used.compareTo(budget) > 0) {
            return BudgetStatus.EXCEEDED;
        }
        if (used.multiply(HUNDRED).compareTo(budget.multiply(WARNING_THRESHOLD_PERCENT)) >= 0) {
            return BudgetStatus.WARNING;
        }
        return BudgetStatus.OK;
    }

    private static BigDecimal percent(BigDecimal used, BigDecimal budget) {
        return used.multiply(HUNDRED).divide(budget, SCALE, RoundingMode.HALF_UP);
    }
}
