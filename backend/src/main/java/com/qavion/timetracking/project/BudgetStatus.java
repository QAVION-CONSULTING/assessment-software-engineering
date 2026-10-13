package com.qavion.timetracking.project;

/** Declared from least to most severe; {@code ordinal()} order is relied on for "most severe". */
public enum BudgetStatus {
    OK,
    WARNING,
    EXCEEDED;

    public static BudgetStatus mostSevere(BudgetStatus a, BudgetStatus b) {
        return a.compareTo(b) >= 0 ? a : b;
    }
}
