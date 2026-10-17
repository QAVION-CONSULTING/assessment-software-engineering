package com.example.timetracker.project;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Pure budget-status calculation, extracted from {@link ProjectService}
 * so it can be unit-tested without Spring or a database.
 *
 * <p>Rules (per the API spec):
 * <ul>
 *   <li>recordedCostCents = round(recordedMinutes / 60 × hourlyRateCents, HALF_UP)</li>
 *   <li>ratios rounded to 4 decimals (HALF_UP), can exceed 1.0</li>
 *   <li>warning = ratio ≥ 0.80 && ratio < 1.0</li>
 *   <li>exceeded = ratio ≥ 1.0</li>
 * </ul>
 */
public final class BudgetCalculator {

    /** Ratio at which the warning flag turns on. */
    public static final BigDecimal WARNING_THRESHOLD = new BigDecimal("0.80");

    private static final int RATIO_SCALE = 4;
    private static final BigDecimal MINUTES_PER_HOUR = BigDecimal.valueOf(60);

    private BudgetCalculator() {
    }

    /**
     * Cost of the recorded work in euro cents:
     * round(recordedMinutes / 60 × hourlyRateCents, HALF_UP).
     */
    public static long recordedCostCents(long recordedMinutes, long hourlyRateCents) {
        return BigDecimal.valueOf(recordedMinutes)
                .divide(MINUTES_PER_HOUR, RATIO_SCALE + 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(hourlyRateCents))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    /** recorded / budget, rounded to 4 decimals (HALF_UP); can exceed 1.0. */
    public static BigDecimal ratio(long recorded, long budget) {
        return BigDecimal.valueOf(recorded)
                .divide(BigDecimal.valueOf(budget), RATIO_SCALE, RoundingMode.HALF_UP);
    }

    /** ratio ≥ 0.80 && ratio < 1.0. */
    public static boolean isWarning(BigDecimal ratio) {
        return ratio.compareTo(WARNING_THRESHOLD) >= 0
                && ratio.compareTo(BigDecimal.ONE) < 0;
    }

    /** ratio ≥ 1.0. */
    public static boolean isExceeded(BigDecimal ratio) {
        return ratio.compareTo(BigDecimal.ONE) >= 0;
    }
}
