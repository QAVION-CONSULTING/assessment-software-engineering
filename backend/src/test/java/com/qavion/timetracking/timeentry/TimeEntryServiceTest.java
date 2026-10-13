package com.qavion.timetracking.timeentry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qavion.timetracking.common.BusinessRuleViolationException;
import com.qavion.timetracking.common.ResourceNotFoundException;
import com.qavion.timetracking.project.ProjectRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TimeEntryServiceTest {

    private static final LocalDate DAY = LocalDate.of(2025, 3, 10);
    private static final LocalDate OTHER_DAY = LocalDate.of(2025, 3, 11);

    @Mock
    TimeEntryRepository entries;
    @Mock
    ProjectRepository projects;
    @InjectMocks
    TimeEntryService service;

    private static TimeEntryRequest request(long projectId, LocalDate date, String hours) {
        return new TimeEntryRequest(projectId, date, new BigDecimal(hours), "work");
    }

    private static TimeEntry persistedEntry(long id, long projectId, LocalDate date, String hours) {
        TimeEntry entry = new TimeEntry(projectId, date, new BigDecimal(hours), null);
        ReflectionTestUtils.setField(entry, "id", id);
        return entry;
    }

    private void projectExists(long id) {
        when(projects.existsById(id)).thenReturn(true);
    }

    // ---- create ----

    @Test
    void createSavesEntryWhenWithinDailyLimit() {
        projectExists(1L);
        when(entries.sumHoursByDate(DAY)).thenReturn(new BigDecimal("8"));
        when(entries.save(any(TimeEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        TimeEntryResponse response = service.create(request(1L, DAY, "4.5"));

        assertThat(response.projectId()).isEqualTo(1L);
        assertThat(response.entryDate()).isEqualTo(DAY);
        assertThat(response.durationHours()).isEqualByComparingTo("4.5");
        assertThat(response.description()).isEqualTo("work");
    }

    @Test
    void createRejectsUnknownProject() {
        when(projects.existsById(7L)).thenReturn(false);

        assertThatThrownBy(() -> service.create(request(7L, DAY, "1")))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("7");
        verify(entries, never()).save(any());
    }

    @Test
    void createRejectsEntryExceedingDailyLimitAcrossProjects() {
        projectExists(2L);
        // e.g. project A 15 h + project B 8 h already booked on that day
        when(entries.sumHoursByDate(DAY)).thenReturn(new BigDecimal("23"));

        assertThatThrownBy(() -> service.create(request(2L, DAY, "2")))
                .isInstanceOf(DailyHoursLimitExceededException.class)
                .hasMessageContaining("23");
        verify(entries, never()).save(any());
    }

    @Test
    void createAllowsExactly24HoursInTotal() {
        projectExists(1L);
        when(entries.sumHoursByDate(DAY)).thenReturn(new BigDecimal("23"));
        when(entries.save(any(TimeEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.create(request(1L, DAY, "1")).durationHours()).isEqualByComparingTo("1");
    }

    // ---- update ----

    @Test
    void updateExcludesOwnPreviousDurationOnSameDate() {
        TimeEntry existing = persistedEntry(5L, 1L, DAY, "10");
        when(entries.findById(5L)).thenReturn(Optional.of(existing));
        projectExists(1L);
        when(entries.sumHoursByDate(DAY)).thenReturn(new BigDecimal("22")); // includes the 10 h of this entry

        // 22 - 10 + 12 = 24 -> allowed
        TimeEntryResponse response = service.update(5L, request(1L, DAY, "12"));

        assertThat(response.durationHours()).isEqualByComparingTo("12");
        assertThat(existing.getDurationHours()).isEqualByComparingTo("12");
    }

    @Test
    void updateStillRejectsWhenNewDurationExceedsLimit() {
        TimeEntry existing = persistedEntry(5L, 1L, DAY, "10");
        when(entries.findById(5L)).thenReturn(Optional.of(existing));
        projectExists(1L);
        when(entries.sumHoursByDate(DAY)).thenReturn(new BigDecimal("22"));

        assertThatThrownBy(() -> service.update(5L, request(1L, DAY, "12.5")))
                .isInstanceOf(DailyHoursLimitExceededException.class);
        assertThat(existing.getDurationHours()).isEqualByComparingTo("10");
    }

    @Test
    void movingEntryToAnotherDateDoesNotSubtractItsOldDuration() {
        TimeEntry existing = persistedEntry(5L, 1L, DAY, "10");
        when(entries.findById(5L)).thenReturn(Optional.of(existing));
        projectExists(1L);
        when(entries.sumHoursByDate(OTHER_DAY)).thenReturn(new BigDecimal("20"));

        // 20 + 10 = 30 > 24 even though the entry is leaving another day
        assertThatThrownBy(() -> service.update(5L, request(1L, OTHER_DAY, "10")))
                .isInstanceOf(DailyHoursLimitExceededException.class);
        assertThat(existing.getEntryDate()).isEqualTo(DAY);
    }

    @Test
    void changingProjectIsAllowedAndDoesNotBypassDailyLimit() {
        TimeEntry existing = persistedEntry(5L, 1L, DAY, "10");
        when(entries.findById(5L)).thenReturn(Optional.of(existing));
        projectExists(2L);
        when(entries.sumHoursByDate(DAY)).thenReturn(new BigDecimal("24"));

        TimeEntryResponse response = service.update(5L, request(2L, DAY, "10"));

        assertThat(response.projectId()).isEqualTo(2L);
    }

    @Test
    void updateOfUnknownEntryFails() {
        when(entries.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(9L, request(1L, DAY, "1")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateRejectsUnknownProject() {
        when(entries.findById(5L)).thenReturn(Optional.of(persistedEntry(5L, 1L, DAY, "1")));
        when(projects.existsById(8L)).thenReturn(false);

        assertThatThrownBy(() -> service.update(5L, request(8L, DAY, "1")))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    // ---- read / delete ----

    @Test
    void getOfUnknownEntryFails() {
        when(entries.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(3L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteRemovesExistingEntry() {
        TimeEntry existing = persistedEntry(5L, 1L, DAY, "1");
        when(entries.findById(5L)).thenReturn(Optional.of(existing));

        service.delete(5L);

        verify(entries).delete(existing);
    }

    @Test
    void deleteOfUnknownEntryFails() {
        when(entries.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(5L)).isInstanceOf(ResourceNotFoundException.class);
        verify(entries, never()).delete(any());
    }

    @Test
    void listPassesFiltersToRepository() {
        when(entries.search(1L, DAY, OTHER_DAY)).thenReturn(List.of(persistedEntry(1L, 1L, DAY, "2")));

        assertThat(service.list(1L, DAY, OTHER_DAY)).extracting(TimeEntryResponse::id).containsExactly(1L);
    }
}
