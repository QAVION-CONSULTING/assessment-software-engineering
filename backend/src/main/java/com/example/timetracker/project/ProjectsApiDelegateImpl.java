package com.example.timetracker.project;

import com.example.timetracker.api.ProjectsApiDelegate;
import com.example.timetracker.model.ProjectInput;
import com.example.timetracker.model.ProjectResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Implementation of the Projects API delegate.
 * Currently returns 501 Not Implemented; business logic to be added.
 */
@Service
public class ProjectsApiDelegateImpl implements ProjectsApiDelegate {

    @Override
    public ResponseEntity<ProjectResponse> createProject(ProjectInput projectInput) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public ResponseEntity<Void> deleteProject(Long id) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public ResponseEntity<ProjectResponse> getProject(Long id) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public ResponseEntity<List<ProjectResponse>> listProjects() {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public ResponseEntity<ProjectResponse> updateProject(Long ifMatch, Long id, ProjectInput projectInput) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }
}
