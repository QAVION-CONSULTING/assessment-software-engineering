package com.qavion.timetracking.timeentry;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/time-entries")
@Tag(name = "Time entries")
public class TimeEntryController {

    private final TimeEntryService service;

    public TimeEntryController(TimeEntryService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Record time", description = "Returns 422 if the project is unknown or the date would exceed 24 recorded hours.")
    public ResponseEntity<TimeEntryResponse> create(@Valid @RequestBody TimeEntryRequest request) {
        TimeEntryResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/api/time-entries/" + created.id())).body(created);
    }

    @GetMapping
    @Operation(summary = "List time entries, newest first", description = "Optional filters: project and an inclusive date range.")
    public List<TimeEntryResponse> list(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.list(projectId, from, to);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a time entry")
    public TimeEntryResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a time entry")
    public TimeEntryResponse update(@PathVariable Long id, @Valid @RequestBody TimeEntryRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a time entry")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
