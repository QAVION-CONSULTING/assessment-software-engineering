package com.example.timetracker.tracking;

import com.example.timetracker.api.TimeEntriesApiDelegate;
import com.example.timetracker.model.TimeEntryInput;
import com.example.timetracker.model.TimeEntryResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Implementation of the Time Entries API delegate.
 * Currently returns 501 Not Implemented; business logic to be added.
 */
@Service
public class TimeEntriesApiDelegateImpl implements TimeEntriesApiDelegate {

    @Override
    public ResponseEntity<TimeEntryResponse> createTimeEntry(TimeEntryInput timeEntryInput) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public ResponseEntity<Void> deleteTimeEntry(Long id) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public ResponseEntity<TimeEntryResponse> getTimeEntry(Long id) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public ResponseEntity<List<TimeEntryResponse>> listTimeEntries(java.time.LocalDate date, Long projectId) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public ResponseEntity<TimeEntryResponse> updateTimeEntry(Long ifMatch, Long id, TimeEntryInput timeEntryInput) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }
}
