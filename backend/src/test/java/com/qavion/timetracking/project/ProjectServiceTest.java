package com.qavion.timetracking.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.qavion.timetracking.common.ResourceNotFoundException;
import com.qavion.timetracking.timeentry.ProjectHours;
import com.qavion.timetracking.timeentry.TimeEntryRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    ProjectRepository projects;
    @Mock
    TimeEntryRepository timeEntries;
    @InjectMocks
    ProjectService service;

    private static ProjectRequest request(String name) {
        return new ProjectRequest(name, new BigDecimal("100"), new BigDecimal("5000"), new BigDecimal("50"));
    }

    private static Project persistedProject(long id, String name) {
        Project project = new Project(name, new BigDecimal("100"), new BigDecimal("10000"), new BigDecimal("10"));
        ReflectionTestUtils.setField(project, "id", id);
        return project;
    }

    @Test
    void createStoresTrimmedNameAndReturnsEmptyBudget() {
        when(projects.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));

        ProjectResponse response = service.create(request("  Website  "));

        assertThat(response.name()).isEqualTo("Website");
        assertThat(response.timeBudgetHours()).isEqualByComparingTo("100");
        assertThat(response.budget().totalHours()).isEqualByComparingTo("0");
        assertThat(response.budget().overallStatus()).isEqualTo(BudgetStatus.OK);
    }

    @Test
    void updateChangesAllFieldsAndReevaluatesBudgetAgainstRecordedHours() {
        Project existing = persistedProject(1L, "Old");
        when(projects.findById(1L)).thenReturn(Optional.of(existing));
        when(timeEntries.sumHoursByProjects(anyCollection())).thenReturn(List.of(new ProjectHours(1L, new BigDecimal("90"))));

        ProjectResponse response = service.update(1L, request("New"));

        assertThat(response.name()).isEqualTo("New");
        assertThat(response.hourlyRateEur()).isEqualByComparingTo("50");
        assertThat(response.costBudgetEur()).isEqualByComparingTo("5000");
        // 90 h of the new 100 h budget -> warning; 4500 EUR of 5000 EUR -> warning
        assertThat(response.budget().overallStatus()).isEqualTo(BudgetStatus.WARNING);
    }

    @Test
    void updateOfUnknownProjectFails() {
        when(projects.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, request("X")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void getOfUnknownProjectFails() {
        when(projects.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(5L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listCalculatesBudgetPerProjectAndTreatsMissingAggregateAsZeroHours() {
        when(projects.findAll(any(Sort.class))).thenReturn(List.of(persistedProject(1L, "Busy"), persistedProject(2L, "Idle")));
        when(timeEntries.sumHoursByProjects(anyCollection())).thenReturn(List.of(new ProjectHours(1L, new BigDecimal("85"))));

        List<ProjectResponse> responses = service.list();

        assertThat(responses).extracting(r -> r.budget().timeStatus())
                .containsExactly(BudgetStatus.WARNING, BudgetStatus.OK);
        assertThat(responses.get(1).budget().totalHours()).isEqualByComparingTo("0");
    }

    @Test
    void listOfNoProjectsSkipsAggregateQuery() {
        when(projects.findAll(any(Sort.class))).thenReturn(List.of());

        assertThat(service.list()).isEmpty();
        verifyNoInteractions(timeEntries);
    }
}
