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
import java.util.List;


@Repository
public interface AuditLogIndexingQueueJPARepository extends JpaRepository<AuditLogIndexingQueue, String> {

    @Query(value = "select * from fhdp_audit_log_indexing_queue where indexed is false and node is null for update skip locked limit :limit", nativeQuery = true)
    List<AuditLogIndexingQueue> findAuditLogEntriesToProcess(int limit);

    @Modifying
    @Query(value = "delete from fhdp_audit_log_indexing_queue where indexed is true and node = :node", nativeQuery = true)
    void deleteByIndexedTrue(String node);
}