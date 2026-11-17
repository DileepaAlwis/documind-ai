package com.documind.workspace;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {

    /** Every lookup by ID also filters by owner. This is the ownership check. */
    Optional<Workspace> findByIdAndUserId(Long id, Long userId);

    List<Workspace> findByUserIdOrderByUpdatedAtDesc(Long userId);

    boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);

    boolean existsByUserIdAndNameIgnoreCaseAndIdNot(Long userId, String name, Long id);

    /** The pattern must already be escaped with '!' (see WorkspaceService.escapeLike). */
    @Query("""
            select w from Workspace w
            where w.userId = :userId
              and (lower(w.name) like lower(concat('%', :q, '%')) escape '!'
                   or lower(coalesce(w.description, '')) like lower(concat('%', :q, '%')) escape '!')
            order by w.updatedAt desc
            """)
    List<Workspace> search(@Param("userId") Long userId, @Param("q") String q);

    /** Document counts per workspace, read straight from the documents table. */
    @Query(value = """
            SELECT d.workspace_id AS workspaceId,
                   COUNT(*) AS documentCount,
                   COUNT(CASE WHEN d.status = 'READY' THEN 1 END) AS readyCount
            FROM documents d
            WHERE d.user_id = :userId AND d.workspace_id IN (:workspaceIds)
            GROUP BY d.workspace_id
            """, nativeQuery = true)
    List<DocumentCounts> countDocuments(@Param("userId") Long userId,
                                        @Param("workspaceIds") Collection<Long> workspaceIds);

    interface DocumentCounts {
        Long getWorkspaceId();

        Long getDocumentCount();

        Long getReadyCount();
    }
}
