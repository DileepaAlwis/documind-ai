package com.documind.workspace;

import java.time.Instant;
import java.time.ZoneOffset;

public record WorkspaceResponse(
        Long id,
        String name,
        String description,
        long documentCount,
        long readyDocumentCount,
        Instant createdAt,
        Instant updatedAt
) {
    public static WorkspaceResponse of(Workspace workspace, long documentCount, long readyDocumentCount) {
        return new WorkspaceResponse(
                workspace.getId(),
                workspace.getName(),
                workspace.getDescription(),
                documentCount,
                readyDocumentCount,
                workspace.getCreatedAt().toInstant(ZoneOffset.UTC),
                workspace.getUpdatedAt().toInstant(ZoneOffset.UTC)
        );
    }
}
