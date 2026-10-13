package com.qavion.timetracking.timeentry;

import com.qavion.timetracking.common.BusinessRuleViolationException;
import com.qavion.timetracking.common.ResourceNotFoundException;
import com.qavion.timetracking.project.ProjectRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Time entry use cases.
 * <p>
 * The 24 h/day rule assumes a single user: hours are summed per date across all projects.
 * Concurrent writes for the same date are serialized with a transaction-scoped advisory lock (see
 * {@link TimeEntryRepository#lockDay}), so parallel requests cannot jointly exceed the limit. With multiple
 * users the lock key would have to include the user.
 */
@Service
@Transactional(readOnly = true)
public class TimeEntryService {

    static final BigDecimal MAX_HOURS_PER_DAY = BigDecimal.valueOf(24);

    private final TimeEntryRepository entries;
    private final ProjectRepository projects;

    public TimeEntryService(TimeEntryRepository entries, ProjectRepository projects) {
        this.entries = entries;
        this.projects = projects;
    }

    @Transactional
    public TimeEntryResponse create(TimeEntryRequest request) {
        requireProject(request.projectId());
        ensureDailyLimit(request, null);
        TimeEntry entry = new TimeEntry(request.projectId(), request.entryDate(), request.durationHours(),
                request.description());
        return TimeEntryResponse.from(entries.save(entry));
    }

    @Transactional
    public TimeEntryResponse update(Long id, TimeEntryRequest request) {
        TimeEntry entry = find(id);
        requireProject(request.projectId());
        ensureDailyLimit(request, entry);
        entry.update(request.projectId(), request.entryDate(), request.durationHours(), request.description());
        return TimeEntryResponse.from(entry);
    }

    @Transactional
    public void delete(Long id) {
        entries.delete(find(id));
    }

    public TimeEntryResponse get(Long id) {
        return TimeEntryResponse.from(find(id));
    }

    public List<TimeEntryResponse> list(Long projectId, LocalDate from, LocalDate to) {
        return entries.search(projectId, from, to).stream().map(TimeEntryResponse::from).toList();
    }

    private TimeEntry find(Long id) {
        return entries.findById(id).orElseThrow(() -> new ResourceNotFoundException("Time entry", id));
    }

    private void requireProject(Long projectId) {
        if (!projects.existsById(projectId)) {
            throw new BusinessRuleViolationException("Project with id %d does not exist.".formatted(projectId));
        }
    }

    /** @param existing the entry being edited; its old duration is not counted if it is on the same date. */
    private void ensureDailyLimit(TimeEntryRequest request, TimeEntry existing) {
        entries.lockDay(request.entryDate().toEpochDay());
        BigDecimal alreadyBooked = entries.sumHoursByDate(request.entryDate());
        if (existing != null && existing.getEntryDate().equals(request.entryDate())) {
            alreadyBooked = alreadyBooked.subtract(existing.getDurationHours());
        }
        if (alreadyBooked.add(request.durationHours()).compareTo(MAX_HOURS_PER_DAY) > 0) {
            throw new DailyHoursLimitExceededException(request.entryDate(), alreadyBooked, request.durationHours(),
                    MAX_HOURS_PER_DAY);
        }
    }
}
