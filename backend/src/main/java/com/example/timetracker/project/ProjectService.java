package com.example.timetracker.project;

import com.example.timetracker.common.NotFoundException;
import com.example.timetracker.common.VersionConflictException;
import com.example.timetracker.model.ProjectInput;
import com.example.timetracker.model.ProjectResponse;
import com.example.timetracker.tracking.TimeEntryRepository;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Project business logic: CRUD plus the computed budget status
 * (recorded minutes/costs, ratios, warning/exceeded flags) defined
 * in the OpenAPI spec.
 */
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final TimeEntryRepository timeEntryRepository;

    @Transactional(readOnly = true)
    public List<ProjectResponse> list() {
        return projectRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse get(Long id) {
        return toResponse(findExisting(id));
    }

    @Transactional
    public ProjectResponse create(ProjectInput input) {
        Project project = Project.builder()
                .name(input.getName())
                .description(input.getDescription())
                .timeBudgetMinutes(input.getTimeBudgetMinutes())
                .costBudgetCents(input.getCostBudgetCents().longValue())
                .hourlyRateCents(input.getHourlyRateCents())
                .build();
        return toResponse(projectRepository.save(project));
    }

    /**
     * Full update guarded by optimistic locking: {@code ifMatch} must
     * equal the currently stored version, otherwise 409.
     */
    @Transactional
    public ProjectResponse update(Long id, Long ifMatch, ProjectInput input) {
        Project project = findExisting(id);
        requireVersionMatch(project.getVersion(), ifMatch);
        project.rename(input.getName());
        project.changeDescription(input.getDescription());
        project.replaceBudgets(input.getTimeBudgetMinutes(),
                input.getCostBudgetCents().longValue(), input.getHourlyRateCents());
        // Flush now so the optimistic-lock version is incremented before
        // the response is built — the client must see the new version.
        projectRepository.saveAndFlush(project);
        return toResponse(project);
    }

    @Transactional
    public void delete(Long id) {
        Project project = findExisting(id);
        projectRepository.delete(project);
    }

    private Project findExisting(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Project not found: " + id));
    }

    private void requireVersionMatch(Long currentVersion, Long ifMatch) {
        if (ifMatch == null || !ifMatch.equals(currentVersion)) {
            throw new VersionConflictException(
                    "Version mismatch: expected " + ifMatch + ", current is " + currentVersion);
        }
    }

    /**
     * Computes the budget status per spec; the math lives in
     * {@link BudgetCalculator} (pure, unit-tested).
     */
    private ProjectResponse toResponse(Project project) {
        long recordedMinutes = timeEntryRepository.totalMinutesForProject(project.getId());

        long recordedCostCents = BudgetCalculator.recordedCostCents(
                recordedMinutes, project.getHourlyRateCents());

        BigDecimal timeRatio = BudgetCalculator.ratio(recordedMinutes, project.getTimeBudgetMinutes());
        BigDecimal costRatio = BudgetCalculator.ratio(recordedCostCents, project.getCostBudgetCents());

        return new ProjectResponse()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .timeBudgetMinutes(project.getTimeBudgetMinutes())
                .costBudgetCents(project.getCostBudgetCents().intValue())
                .hourlyRateCents(project.getHourlyRateCents())
                .version(project.getVersion())
                .recordedMinutes((int) recordedMinutes)
                .recordedCostCents((int) recordedCostCents)
                .timeBudgetRatio(timeRatio)
                .costBudgetRatio(costRatio)
                .timeBudgetWarning(BudgetCalculator.isWarning(timeRatio))
                .timeBudgetExceeded(BudgetCalculator.isExceeded(timeRatio))
                .costBudgetWarning(BudgetCalculator.isWarning(costRatio))
                .costBudgetExceeded(BudgetCalculator.isExceeded(costRatio));
    }
}
