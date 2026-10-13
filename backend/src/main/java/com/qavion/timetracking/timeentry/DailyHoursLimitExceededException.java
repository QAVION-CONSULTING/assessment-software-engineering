package com.qavion.timetracking.timeentry;

import com.qavion.timetracking.common.BusinessRuleViolationException;
import java.math.BigDecimal;
import java.time.LocalDate;

public class DailyHoursLimitExceededException extends BusinessRuleViolationException {

    public DailyHoursLimitExceededException(LocalDate date, BigDecimal alreadyBooked, BigDecimal requested,
            BigDecimal maxHours) {
        super("Cannot record %s h on %s: %s h are already booked and the daily maximum is %s h."
                .formatted(requested.stripTrailingZeros().toPlainString(), date,
                        alreadyBooked.stripTrailingZeros().toPlainString(), maxHours.toPlainString()));
    }
}
