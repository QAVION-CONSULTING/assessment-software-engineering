package com.example.timetracker.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.math.BigDecimal;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Unit tests for the pure budget-status calculation, covering the
 * boundary table from docs/TESTING.md:
 * 0% → no flag, 79.99% → no flag, exactly 80% → warning,
 * 99.99% → warning, exactly 100% → exceeded, above 100% → exceeded.
 */
@DisplayName("BudgetCalculator")
class BudgetCalculatorTest {

    @Nested
    @DisplayName("warning/exceeded flags")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class Flags {

        /** One parameter set per boundary row: (ratio, warning, exceeded). */
        Stream<org.junit.jupiter.params.provider.Arguments> boundaries() {
            return Stream.of(
                    org.junit.jupiter.params.provider.Arguments.of("0.0000", false, false),
                    org.junit.jupiter.params.provider.Arguments.of("0.7999", false, false),
                    org.junit.jupiter.params.provider.Arguments.of("0.8000", true, false),
                    org.junit.jupiter.params.provider.Arguments.of("0.9999", true, false),
                    org.junit.jupiter.params.provider.Arguments.of("1.0000", false, true),
                    org.junit.jupiter.params.provider.Arguments.of("1.5000", false, true));
        }

        @ParameterizedTest(name = "ratio {0} → warning={1}, exceeded={2}")
        @MethodSource("boundaries")
        void flagsFollowTheBoundaryTable(String ratio, boolean warning, boolean exceeded) {
            BigDecimal r = new BigDecimal(ratio);
            assertThat(BudgetCalculator.isWarning(r)).as("warning").isEqualTo(warning);
            assertThat(BudgetCalculator.isExceeded(r)).as("exceeded").isEqualTo(exceeded);
        }

        @Test
        void timeAndCostFlagsAreIndependent() {
            // time over budget, cost under warning threshold
            BigDecimal timeRatio = new BigDecimal("1.2000");
            BigDecimal costRatio = new BigDecimal("0.5000");
            assertThat(BudgetCalculator.isExceeded(timeRatio)).isTrue();
            assertThat(BudgetCalculator.isWarning(costRatio)).isFalse();
            assertThat(BudgetCalculator.isExceeded(costRatio)).isFalse();
        }
    }

    @Nested
    @DisplayName("ratio")
    class Ratio {

        @ParameterizedTest(name = "{0}/{1} → {2}")
        @CsvSource({
                "0, 600, 0.0000",
                "480, 600, 0.8000",   // exactly 80%
                "495, 600, 0.8250",
                "600, 600, 1.0000",   // exactly 100%
                "720, 600, 1.2000"    // above 100%
        })
        void computesRatioAtScale4(long recorded, long budget, String expected) {
            assertThat(BudgetCalculator.ratio(recorded, budget))
                    .isEqualByComparingTo(new BigDecimal(expected));
        }

        @Test
        void roundsHalfUpAtFourDecimals() {
            // 2/3 = 0.666666... → 0.6667
            assertThat(BudgetCalculator.ratio(2, 3))
                    .isEqualByComparingTo(new BigDecimal("0.6667"));
        }
    }

    @Nested
    @DisplayName("recordedCostCents")
    class Cost {

        @Test
        void exactHoursMultiplyCleanly() {
            // 480 min = 8 h × 8000 cents/h = 64000 cents
            assertThat(BudgetCalculator.recordedCostCents(480, 8000)).isEqualTo(64_000L);
        }

        @Test
        void fractionalHoursRoundHalfUp() {
            // 450 min = 7.5 h × 8000 = 60000 exactly
            assertThat(BudgetCalculator.recordedCostCents(450, 8000)).isEqualTo(60_000L);
            // 451 min = 7.516666... h × 8000 = 60133.33... → 60133
            assertThat(BudgetCalculator.recordedCostCents(451, 8000))
                    .isCloseTo(60_133L, within(1L));
            // 1 min = 1/60 h × 8000 = 133.33... → 133
            assertThat(BudgetCalculator.recordedCostCents(1, 8000)).isEqualTo(133L);
        }

        @Test
        void zeroMinutesCostNothing() {
            assertThat(BudgetCalculator.recordedCostCents(0, 8000)).isZero();
        }
    }
}
