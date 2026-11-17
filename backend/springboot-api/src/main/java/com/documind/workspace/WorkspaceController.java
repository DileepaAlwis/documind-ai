package com.documind.workspace;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.documind.security.CurrentUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/workspaces")
public class WorkspaceController {

    private final WorkspaceService service;

    public WorkspaceController(WorkspaceService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkspaceResponse create(@AuthenticationPrincipal Jwt jwt,
                                    @Valid @RequestBody WorkspaceRequest request) {
        return service.create(CurrentUser.id(jwt), request);
    }

    @GetMapping
    public List<WorkspaceResponse> list(@AuthenticationPrincipal Jwt jwt,
                                        @RequestParam(name = "q", required = false) String query) {
        return service.list(CurrentUser.id(jwt), query);
    }

    @GetMapping("/{id}")
    public WorkspaceResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") Long id) {
        return service.get(CurrentUser.id(jwt), id);
    }

    @PutMapping("/{id}")
    public WorkspaceResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") Long id,
                                    @Valid @RequestBody WorkspaceRequest request) {
        return service.update(CurrentUser.id(jwt), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") Long id) {
        service.delete(CurrentUser.id(jwt), id);
    }
}

