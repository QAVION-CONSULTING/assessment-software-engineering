package com.example.timetracker.project;

import com.example.timetracker.api.ProjectsApiDelegate;
import com.example.timetracker.model.ProjectInput;
import com.example.timetracker.model.ProjectResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Delegate implementation for the Projects API; delegates to
 * {@link ProjectService} and maps results to HTTP responses.
 */
@Service
@RequiredArgsConstructor
public class ProjectsApiDelegateImpl implements ProjectsApiDelegate {

    private final ProjectService projectService;

    @Override
    public ResponseEntity<ProjectResponse> createProject(ProjectInput projectInput) {
        ProjectResponse created = projectService.create(projectInput);
        return ResponseEntity
                .created(ServletUriComponentsBuilder.fromCurrentRequest()
                        .path("/{id}").buildAndExpand(created.getId()).toUri())
                .body(created);
    }

    @Override
    public ResponseEntity<Void> deleteProject(Long id) {
        projectService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<ProjectResponse> getProject(Long id) {
        return ResponseEntity.ok(projectService.get(id));
    }

    @Override
    public ResponseEntity<List<ProjectResponse>> listProjects() {
        return ResponseEntity.ok(projectService.list());
    }

    @Override
    public ResponseEntity<ProjectResponse> updateProject(Long ifMatch, Long id, ProjectInput projectInput) {
        return ResponseEntity.ok(projectService.update(id, ifMatch, projectInput));
    }
}
