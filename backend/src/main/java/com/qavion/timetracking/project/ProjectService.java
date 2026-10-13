package com.qavion.timetracking.project;

import com.qavion.timetracking.common.ResourceNotFoundException;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectRepository projects;

    public ProjectService(ProjectRepository projects) {
        this.projects = projects;
    }

    @Transactional
    public ProjectResponse create(ProjectRequest request) {
        Project project = new Project(request.name().strip(), request.timeBudgetHours(),
                request.costBudgetEur(), request.hourlyRateEur());
        return ProjectResponse.from(projects.save(project));
    }

    @Transactional
    public ProjectResponse update(Long id, ProjectRequest request) {
        Project project = find(id);
        project.update(request.name().strip(), request.timeBudgetHours(),
                request.costBudgetEur(), request.hourlyRateEur());
        return ProjectResponse.from(project);
    }

    public ProjectResponse get(Long id) {
        return ProjectResponse.from(find(id));
    }

    public List<ProjectResponse> list() {
        return projects.findAll(Sort.by("name", "id")).stream().map(ProjectResponse::from).toList();
    }

    private Project find(Long id) {
        return projects.findById(id).orElseThrow(() -> new ResourceNotFoundException("Project", id));
    }
}
