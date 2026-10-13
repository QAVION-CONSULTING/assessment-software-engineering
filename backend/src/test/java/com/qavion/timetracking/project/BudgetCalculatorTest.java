package com.qavion.timetracking.project;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BudgetCalculatorTest {

    /** 100 h time budget, 10 000 EUR cost budget, 1 EUR/h: cost never interferes unless a test wants it to. */
    private static Project project(String timeBudget, String costBudget, String rate) {
        return new Project("P", new BigDecimal(timeBudget), new BigDecimal(costBudget), new BigDecimal(rate));
    }

    @ParameterizedTest(name = "{0} h of 100 h -> {1}")
    @CsvSource({
            "0, OK",
            "79.99, OK",
            "80, WARNING",
            "80.01, WARNING",
            "100, WARNING",
            "100.01, EXCEEDED",
            "150, EXCEEDED"
    })
    void timeStatusFollowsThresholds(String hours, BudgetStatus expected) {
        BudgetSummary summary = BudgetCalculator.calculate(project("100", "100000", "1"), new BigDecimal(hours));

        assertThat(summary.timeStatus()).isEqualTo(expected);
        assertThat(summary.costStatus()).isEqualTo(BudgetStatus.OK);
        assertThat(summary.overallStatus()).isEqualTo(expected);
    }

    @ParameterizedTest(name = "cost {0} EUR of 1000 EUR -> {1}")
    @CsvSource({
            "799.99, OK",
            "800, WARNING",
            "1000, WARNING",
            "1000.01, EXCEEDED"
    })
    void costStatusFollowsThresholds(String cost, BudgetStatus expected) {
        // rate 1 EUR/h so that hours == cost; time budget is huge and stays OK
        BudgetSummary summary = BudgetCalculator.calculate(project("100000", "1000", "1"), new BigDecimal(cost));

        assertThat(summary.costStatus()).isEqualTo(expected);
        assertThat(summary.timeStatus()).isEqualTo(BudgetStatus.OK);
        assertThat(summary.overallStatus()).isEqualTo(expected);
    }

    @Test
    void statusIsDecidedOnExactValuesNotOnRoundedPercentage() {
        // 79.996 % rounds to 80.00 % for display but has not reached 80 %
        BudgetSummary summary = BudgetCalculator.calculate(project("100000", "100000", "1"),
                new BigDecimal("79996"));

        assertThat(summary.timeUtilizationPercent()).isEqualByComparingTo("80.00");
        assertThat(summary.timeStatus()).isEqualTo(BudgetStatus.OK);
    }

    @Test
    void overallStatusIsMostSevereOfTimeAndCost() {
        // 50 h of 100 h (OK) but 50 h * 30 EUR = 1500 EUR of 1000 EUR (EXCEEDED)
        BudgetSummary summary = BudgetCalculator.calculate(project("100", "1000", "30"), new BigDecimal("50"));

        assertThat(summary.timeStatus()).isEqualTo(BudgetStatus.OK);
        assertThat(summary.costStatus()).isEqualTo(BudgetStatus.EXCEEDED);
        assertThat(summary.overallStatus()).isEqualTo(BudgetStatus.EXCEEDED);
    }

    @Test
    void timeAndCostCanWarnIndependently() {
        // 90 h of 100 h (WARNING) but 90 h * 1 EUR = 90 EUR of 1000 EUR (OK)
        BudgetSummary summary = BudgetCalculator.calculate(project("100", "1000", "1"), new BigDecimal("90"));

        assertThat(summary.timeStatus()).isEqualTo(BudgetStatus.WARNING);
        assertThat(summary.costStatus()).isEqualTo(BudgetStatus.OK);
        assertThat(summary.overallStatus()).isEqualTo(BudgetStatus.WARNING);
    }

    @Test
    void calculatesTotalsAndPercentages() {
        BudgetSummary summary = BudgetCalculator.calculate(project("40", "2000", "75.50"), new BigDecimal("12.25"));

        assertThat(summary.totalHours()).isEqualByComparingTo("12.25");
        assertThat(summary.totalCostEur()).isEqualByComparingTo("924.88"); // 12.25 * 75.50 = 924.875 -> HALF_UP
        assertThat(summary.timeUtilizationPercent()).isEqualByComparingTo("30.63"); // 30.625
        assertThat(summary.costUtilizationPercent()).isEqualByComparingTo("46.24"); // 46.24375
    }

    @Test
    void zeroRecordedHoursYieldZeroCostAndOkStatus() {
        BudgetSummary summary = BudgetCalculator.calculate(project("100", "1000", "50"), BigDecimal.ZERO);

        assertThat(summary.totalHours()).isEqualByComparingTo("0");
        assertThat(summary.totalCostEur()).isEqualByComparingTo("0");
        assertThat(summary.timeUtilizationPercent()).isEqualByComparingTo("0");
        assertThat(summary.costUtilizationPercent()).isEqualByComparingTo("0");
        assertThat(summary.overallStatus()).isEqualTo(BudgetStatus.OK);
    }

    @Test
    void zeroHourlyRateMeansZeroCost() {
        BudgetSummary summary = BudgetCalculator.calculate(project("100", "1000", "0"), new BigDecimal("60"));

        assertThat(summary.totalCostEur()).isEqualByComparingTo("0");
        assertThat(summary.costStatus()).isEqualTo(BudgetStatus.OK);
    }
}
