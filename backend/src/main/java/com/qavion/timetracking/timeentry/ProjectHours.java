package com.qavion.timetracking.timeentry;

import java.math.BigDecimal;

/** Total recorded hours of one project (aggregate query result). */
public record ProjectHours(Long projectId, BigDecimal totalHours) {
}
