package com.example.timetracker.tracking;

import com.example.timetracker.api.TimeEntriesApiDelegate;
import com.example.timetracker.model.TimeEntryInput;
import com.example.timetracker.model.TimeEntryResponse;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Delegate implementation for the Time Entries API; delegates to
 * {@link TimeEntryService} and maps results to HTTP responses.
 */
@Service
@RequiredArgsConstructor
public class TimeEntriesApiDelegateImpl implements TimeEntriesApiDelegate {

    private final TimeEntryService timeEntryService;

    @Override
    public ResponseEntity<TimeEntryResponse> createTimeEntry(TimeEntryInput timeEntryInput) {
        TimeEntryResponse created = timeEntryService.create(timeEntryInput);
        return ResponseEntity
                .created(ServletUriComponentsBuilder.fromCurrentRequest()
                        .path("/{id}").buildAndExpand(created.getId()).toUri())
                .body(created);
    }

    @Override
    public ResponseEntity<Void> deleteTimeEntry(Long id) {
        timeEntryService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<TimeEntryResponse> getTimeEntry(Long id) {
        return ResponseEntity.ok(timeEntryService.get(id));
    }

    @Override
    public ResponseEntity<List<TimeEntryResponse>> listTimeEntries(LocalDate date, Long projectId) {
        return ResponseEntity.ok(timeEntryService.list(date, projectId));
    }

    @Override
    public ResponseEntity<TimeEntryResponse> updateTimeEntry(Long ifMatch, Long id, TimeEntryInput timeEntryInput) {
        return ResponseEntity.ok(timeEntryService.update(id, ifMatch, timeEntryInput));
    }
}
