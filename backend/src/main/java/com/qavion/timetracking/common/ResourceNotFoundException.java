package com.qavion.timetracking.common;

/** Thrown when a requested resource does not exist; mapped to HTTP 404. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Long id) {
        super("%s with id %d not found.".formatted(resource, id));
    }
}
