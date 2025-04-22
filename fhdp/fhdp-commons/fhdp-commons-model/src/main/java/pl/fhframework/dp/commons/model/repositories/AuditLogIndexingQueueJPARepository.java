package pl.fhframework.dp.commons.model.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.fhframework.dp.commons.model.entities.AuditLogIndexingQueue;

import java.time.LocalDateTime;


@Repository
public interface AuditLogIndexingQueueJPARepository extends JpaRepository<AuditLogIndexingQueue, String> {
    Page<AuditLogIndexingQueue> findByIndexedAndNode(boolean indexed, String nodeId, Pageable pageable);

    void deleteByIndexedTrue();

    @Modifying
    @Query(value = "UPDATE fhdp_audit_log_indexing_queue SET node = :node WHERE id IN (SELECT id FROM fhdp_audit_log_indexing_queue WHERE indexed = false ORDER BY eventtime LIMIT 300)", nativeQuery = true)
    void markForIndexing(@Param("node") String nodeId);

    @Modifying
    @Query(value = "update fhdp_audit_log_indexing_queue set indexed = true, indexingTime = :now where indexed = false and node = :node", nativeQuery = true)
    void updateIndexed(@Param("node") String nodeId, @Param("now") LocalDateTime now);
}