package com.example.timetracker.project;

import com.example.timetracker.common.NotFoundException;
import com.example.timetracker.common.VersionConflictException;
import com.example.timetracker.model.ProjectInput;
import com.example.timetracker.model.ProjectResponse;
import com.example.timetracker.tracking.TimeEntryRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
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

    private static final BigDecimal WARNING_THRESHOLD = new BigDecimal("0.80");
    private static final int RATIO_SCALE = 4;

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
        return toResponse(project); // dirty checking flushes and bumps @Version
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
     * Computes the budget status per spec:
     * recordedCostCents = round(recordedMinutes / 60 * hourlyRateCents, HALF_UP),
     * ratios at scale 4 (can exceed 1.0),
     * warning = ratio >= 0.80 && ratio < 1.0, exceeded = ratio >= 1.0.
     */
    private ProjectResponse toResponse(Project project) {
        long recordedMinutes = timeEntryRepository.totalMinutesForProject(project.getId());

        long recordedCostCents = BigDecimal.valueOf(recordedMinutes)
                .divide(BigDecimal.valueOf(60), RATIO_SCALE + 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(project.getHourlyRateCents()))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();

        BigDecimal timeRatio = ratio(recordedMinutes, project.getTimeBudgetMinutes());
        BigDecimal costRatio = ratio(recordedCostCents, project.getCostBudgetCents());

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
                .timeBudgetWarning(isWarning(timeRatio))
                .timeBudgetExceeded(isExceeded(timeRatio))
                .costBudgetWarning(isWarning(costRatio))
                .costBudgetExceeded(isExceeded(costRatio));
    }

    private static BigDecimal ratio(long recorded, long budget) {
        return BigDecimal.valueOf(recorded)
                .divide(BigDecimal.valueOf(budget), RATIO_SCALE, RoundingMode.HALF_UP);
    }

    private static boolean isWarning(BigDecimal ratio) {
        return ratio.compareTo(WARNING_THRESHOLD) >= 0 && ratio.compareTo(BigDecimal.ONE) < 0;
    }

    private static boolean isExceeded(BigDecimal ratio) {
        return ratio.compareTo(BigDecimal.ONE) >= 0;
    }
}
