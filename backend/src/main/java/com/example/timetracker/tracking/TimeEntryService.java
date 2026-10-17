package com.example.timetracker.tracking;

import com.example.timetracker.common.BusinessRuleException;
import com.example.timetracker.common.NotFoundException;
import com.example.timetracker.common.VersionConflictException;
import com.example.timetracker.model.TimeEntryInput;
import com.example.timetracker.model.TimeEntryResponse;
import com.example.timetracker.project.Project;
import com.example.timetracker.project.ProjectRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Time entry business logic: CRUD plus the daily-cap rule (max 24 h
 * per date, counting existing entries) and project existence checks.
 */
@Service
@RequiredArgsConstructor
public class TimeEntryService {

    private final TimeEntryRepository timeEntryRepository;
    private final ProjectRepository projectRepository;

    @Transactional(readOnly = true)
    public List<TimeEntryResponse> list(LocalDate date, Long projectId) {
        return timeEntryRepository.findByFilters(date, projectId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TimeEntryResponse get(Long id) {
        return toResponse(findExisting(id));
    }

    @Transactional
    public TimeEntryResponse create(TimeEntryInput input) {
        Project project = findProject(input.getProjectId());
        requireDurationBounds(input.getDurationMinutes());
        int alreadyRecorded = timeEntryRepository.totalMinutesForDate(input.getDate());
        requireDailyCap(input.getDate(), alreadyRecorded, input.getDurationMinutes());

        TimeEntry entry = TimeEntry.builder()
                .date(input.getDate())
                .durationMinutes(input.getDurationMinutes())
                .description(input.getDescription())
                .project(project)
                .build();
        return toResponse(timeEntryRepository.save(entry));
    }

    /**
     * Full update guarded by optimistic locking. The daily-cap check
     * excludes the entry being updated (its old duration must not count).
     */
    @Transactional
    public TimeEntryResponse update(Long id, Long ifMatch, TimeEntryInput input) {
        TimeEntry entry = findExisting(id);
        requireVersionMatch(entry.getVersion(), ifMatch);

        Project project = findProject(input.getProjectId());
        requireDurationBounds(input.getDurationMinutes());
        // The daily-cap sum must exclude the entry being edited — its old
        // duration would otherwise be counted twice.
        int alreadyRecorded = timeEntryRepository.totalMinutesForDateExcluding(
                input.getDate(), id);
        requireDailyCap(input.getDate(), alreadyRecorded, input.getDurationMinutes());

        entry.replace(input.getDate(), input.getDurationMinutes(), input.getDescription(), project);
        // Flush so the optimistic-lock version is incremented before the
        // response is built — the client must see the new version.
        timeEntryRepository.saveAndFlush(entry);
        return toResponse(entry);
    }

    @Transactional
    public void delete(Long id) {
        TimeEntry entry = findExisting(id);
        timeEntryRepository.delete(entry);
    }

    private void requireDurationBounds(Integer durationMinutes) {
        if (durationMinutes == null || durationMinutes < 1) {
            throw new BusinessRuleException("Duration must be greater than 0 minutes");
        }
        if (durationMinutes > TimeEntry.MAX_DURATION_MINUTES) {
            throw new BusinessRuleException("A single entry cannot exceed 24 hours");
        }
    }

    /**
     * The daily cap: sum of all entries for a date plus the new duration
     * must not exceed 1440 min (24 h). Entries across different projects
     * on the same date share the cap.
     */
    private void requireDailyCap(LocalDate date, int alreadyRecorded, int additional) {
        if (alreadyRecorded + additional > TimeEntry.MAX_DURATION_MINUTES) {
            throw new BusinessRuleException(
                    "Total time for %s would exceed 24 h (already recorded: %.2f h)"
                            .formatted(date, alreadyRecorded / 60.0));
        }
    }

    private Project findProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessRuleException("Project not found: " + projectId));
    }

    private TimeEntry findExisting(Long id) {
        return timeEntryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Time entry not found: " + id));
    }

    private void requireVersionMatch(Long currentVersion, Long ifMatch) {
        if (ifMatch == null || !ifMatch.equals(currentVersion)) {
            throw new VersionConflictException(
                    "Version mismatch: expected " + ifMatch + ", current is " + currentVersion);
        }
    }

    private TimeEntryResponse toResponse(TimeEntry entry) {
        return new TimeEntryResponse()
                .id(entry.getId())
                .date(entry.getDate())
                .projectId(entry.getProject().getId())
                .projectName(entry.getProject().getName())
                .durationMinutes(entry.getDurationMinutes())
                .version(entry.getVersion())
                .description(entry.getDescription());
    }
}
