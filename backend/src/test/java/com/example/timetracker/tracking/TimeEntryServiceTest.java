package com.example.timetracker.tracking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.timetracker.common.BusinessRuleException;
import com.example.timetracker.common.NotFoundException;
import com.example.timetracker.model.TimeEntryInput;
import com.example.timetracker.model.TimeEntryResponse;
import com.example.timetracker.project.Project;
import com.example.timetracker.project.ProjectRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for the time-entry service decisions, with mocked
 * repositories. These test the interesting logic around the daily cap —
 * which query is used with which arguments, in which order checks run,
 * and what is persisted 
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TimeEntryService")
class TimeEntryServiceTest {

    private static final LocalDate DATE = LocalDate.of(2026, 10, 17);

    @Mock
    private TimeEntryRepository timeEntryRepository;

    @Mock
    private ProjectRepository projectRepository;

    private TimeEntryService service;

    private Project project;

    @BeforeEach
    void setUp() {
        service = new TimeEntryService(timeEntryRepository, projectRepository);
        project = Project.builder()
                .id(1L).name("Website Relaunch")
                .timeBudgetMinutes(600).costBudgetCents(960_000L).hourlyRateCents(8000)
                .build();
        lenient().when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
    }

    private TimeEntryInput input(int durationMinutes) {
        TimeEntryInput input = new TimeEntryInput();
        input.setDate(DATE);
        input.setProjectId(1L);
        input.setDurationMinutes(durationMinutes);
        input.setDescription("work");
        return input;
    }

    @Nested
    @DisplayName("create — daily cap")
    class CreateDailyCap {

        @Test
        @DisplayName("create sums ALL entries for the date (not per project) and allows when under cap")
        void createSumsAllEntriesForTheDate() {
            // 1400 min already recorded on the date (across any projects)
            when(timeEntryRepository.totalMinutesForDate(DATE)).thenReturn(1400);
            when(timeEntryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            TimeEntryResponse response = service.create(input(40));

            assertThat(response.getDurationMinutes()).isEqualTo(40);
            verify(timeEntryRepository).save(any(TimeEntry.class));
        }

        @Test
        @DisplayName("rejects when the daily total would exceed 24 h, with hours in the message")
        void createRejectsOverCap() {
            when(timeEntryRepository.totalMinutesForDate(DATE)).thenReturn(1400);

            assertThatThrownBy(() -> service.create(input(41)))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Total time for 2026-10-17 would exceed 24 h (already recorded: 23,33 h)");

            verify(timeEntryRepository, never()).save(any());
        }

        @Test
        @DisplayName("boundary: exactly 1440 total is allowed")
        void createAllowsExactCap() {
            when(timeEntryRepository.totalMinutesForDate(DATE)).thenReturn(1440);
            assertThatThrownBy(() -> service.create(input(1)))
                    .isInstanceOf(BusinessRuleException.class);

            when(timeEntryRepository.totalMinutesForDate(DATE)).thenReturn(1439);
            when(timeEntryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            service.create(input(1)); // does not throw
        }

        @Test
        @DisplayName("checks the project BEFORE the cap: unknown project → 422, no cap query")
        void createChecksProjectFirst() {
            when(projectRepository.findById(999L)).thenReturn(Optional.empty());
            TimeEntryInput unknown = input(60);
            unknown.setProjectId(999L);

            assertThatThrownBy(() -> service.create(unknown))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Project not found: 999");

            verify(timeEntryRepository, never()).totalMinutesForDate(any());
        }
    }

    @Nested
    @DisplayName("duration bounds")
    class DurationBounds {

        @Test
        @DisplayName("single entry over 24 h → 422, before any cap query")
        void singleEntryOver24h() {
            assertThatThrownBy(() -> service.create(input(1441)))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("A single entry cannot exceed 24 hours");

            verify(timeEntryRepository, never()).totalMinutesForDate(any());
        }

        @Test
        @DisplayName("zero/negative duration → 422")
        void zeroOrNegativeDuration() {
            assertThatThrownBy(() -> service.create(input(0)))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Duration must be greater than 0 minutes");
        }
    }

    @Nested
    @DisplayName("persistence mapping")
    class Persistence {

        @Test
        @DisplayName("create persists date, duration, description and project reference")
        void createPersistsCorrectFields() {
            when(timeEntryRepository.totalMinutesForDate(DATE)).thenReturn(0);
            when(timeEntryRepository.save(any())).thenAnswer(inv -> {
                TimeEntry e = inv.getArgument(0);
                return TimeEntry.builder()
                        .id(42L).date(e.getDate()).durationMinutes(e.getDurationMinutes())
                        .description(e.getDescription()).project(e.getProject()).build();
            });

            service.create(input(450));

            ArgumentCaptor<TimeEntry> captor = ArgumentCaptor.forClass(TimeEntry.class);
            verify(timeEntryRepository).save(captor.capture());
            TimeEntry saved = captor.getValue();
            assertThat(saved.getDate()).isEqualTo(DATE);
            assertThat(saved.getDurationMinutes()).isEqualTo(450);
            assertThat(saved.getDescription()).isEqualTo("work");
            assertThat(saved.getProject().getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("unknown entry id → 404")
        void getUnknownId() {
            when(timeEntryRepository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.get(404L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Time entry not found: 404");
        }
    }
}
