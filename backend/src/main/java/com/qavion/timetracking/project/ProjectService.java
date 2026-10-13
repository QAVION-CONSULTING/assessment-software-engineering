package com.qavion.timetracking.project;

import com.qavion.timetracking.common.ResourceNotFoundException;
import com.qavion.timetracking.timeentry.ProjectHours;
import com.qavion.timetracking.timeentry.TimeEntryRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectRepository projects;
    private final TimeEntryRepository timeEntries;

    public ProjectService(ProjectRepository projects, TimeEntryRepository timeEntries) {
        this.projects = projects;
        this.timeEntries = timeEntries;
    }

    @Transactional
    public ProjectResponse create(ProjectRequest request) {
        Project project = new Project(request.name().strip(), request.timeBudgetHours(),
                request.costBudgetEur(), request.hourlyRateEur());
        // A new project cannot have time entries yet.
        return ProjectResponse.from(projects.save(project), BigDecimal.ZERO);
    }

    @Transactional
    public ProjectResponse update(Long id, ProjectRequest request) {
        Project project = find(id);
        project.update(request.name().strip(), request.timeBudgetHours(),
                request.costBudgetEur(), request.hourlyRateEur());
        return toResponses(List.of(project)).getFirst();
    }

    public ProjectResponse get(Long id) {
        return toResponses(List.of(find(id))).getFirst();
    }

    public List<ProjectResponse> list() {
        return toResponses(projects.findAll(Sort.by("name", "id")));
    }

    private Project find(Long id) {
        return projects.findById(id).orElseThrow(() -> new ResourceNotFoundException("Project", id));
    }

    /** Resolves hours for all projects with one grouped query. */
    private List<ProjectResponse> toResponses(List<Project> list) {
        if (list.isEmpty()) {
            return List.of();
        }
        Map<Long, BigDecimal> hoursByProject = timeEntries
                .sumHoursByProjects(list.stream().map(Project::getId).toList()).stream()
                .collect(Collectors.toMap(ProjectHours::projectId, ProjectHours::totalHours));
        return list.stream()
                .map(p -> ProjectResponse.from(p, hoursByProject.getOrDefault(p.getId(), BigDecimal.ZERO)))
                .toList();
    }
}
