package com.documind.workspace;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.documind.common.ApiException;
import com.documind.common.ResourceNotFoundException;
import com.documind.workspace.WorkspaceRepository.DocumentCounts;

@Service
public class WorkspaceService {

    private final WorkspaceRepository workspaces;

    public WorkspaceService(WorkspaceRepository workspaces) {
        this.workspaces = workspaces;
    }

    @Transactional(readOnly = true)
    public List<WorkspaceResponse> list(Long userId, String query) {
        List<Workspace> found = (query == null || query.isBlank())
                ? workspaces.findByUserIdOrderByUpdatedAtDesc(userId)
                : workspaces.search(userId, escapeLike(query.trim()));
        return withStats(userId, found);
    }

    @Transactional(readOnly = true)
    public WorkspaceResponse get(Long userId, Long id) {
        return withStats(userId, List.of(findOwned(userId, id))).get(0);
    }

    @Transactional
    public WorkspaceResponse create(Long userId, WorkspaceRequest request) {
        String name = request.name().trim();
        if (workspaces.existsByUserIdAndNameIgnoreCase(userId, name)) {
            throw nameTaken();
        }
        Workspace workspace = new Workspace();
        workspace.setUserId(userId);
        workspace.setName(name);
        workspace.setDescription(clean(request.description()));
        try {
            workspace = workspaces.saveAndFlush(workspace);
        } catch (DataIntegrityViolationException e) {
            throw nameTaken();   // same name created at the same moment
        }
        return WorkspaceResponse.of(workspace, 0, 0);
    }

    @Transactional
    public WorkspaceResponse update(Long userId, Long id, WorkspaceRequest request) {
        Workspace workspace = findOwned(userId, id);
        String name = request.name().trim();
        if (workspaces.existsByUserIdAndNameIgnoreCaseAndIdNot(userId, name, id)) {
            throw nameTaken();
        }
        workspace.setName(name);
        workspace.setDescription(clean(request.description()));
        try {
            workspaces.saveAndFlush(workspace);
        } catch (DataIntegrityViolationException e) {
            throw nameTaken();
        }
        return withStats(userId, List.of(workspace)).get(0);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        Workspace workspace = findOwned(userId, id);
        // TODO (Day 5+): before deleting the rows, remove every document's stored file and its
        // Qdrant vectors (see ARCHITECTURE.md, section 5). The database cascade only removes rows.
        workspaces.delete(workspace);
    }

    // ---------------- helpers ----------------

    private Workspace findOwned(Long userId, Long id) {
        return workspaces.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found."));
    }

    private List<WorkspaceResponse> withStats(Long userId, List<Workspace> list) {
        if (list.isEmpty()) {
            return List.of();
        }
        Map<Long, DocumentCounts> counts = workspaces
                .countDocuments(userId, list.stream().map(Workspace::getId).toList())
                .stream()
                .collect(Collectors.toMap(DocumentCounts::getWorkspaceId, Function.identity()));

        return list.stream().map(workspace -> {
            DocumentCounts c = counts.get(workspace.getId());
            return WorkspaceResponse.of(workspace,
                    c == null ? 0 : c.getDocumentCount(),
                    c == null ? 0 : c.getReadyCount());
        }).toList();
    }

    /** Makes '%' and '_' in the search text match literally. Pairs with "escape '!'" in the query. */
    private static String escapeLike(String value) {
        return value.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }

    private static String clean(String description) {
        return (description == null || description.isBlank()) ? null : description.trim();
    }

    private static ApiException nameTaken() {
        return new ApiException(HttpStatus.CONFLICT, "A workspace with this name already exists.");
    }
}
