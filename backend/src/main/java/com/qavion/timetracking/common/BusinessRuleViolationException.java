package com.qavion.timetracking.common;

/** Thrown when a syntactically valid request violates a business rule; mapped to HTTP 422. */
public class BusinessRuleViolationException extends RuntimeException {

    public BusinessRuleViolationException(String message) {
        super(message);
    }
}
