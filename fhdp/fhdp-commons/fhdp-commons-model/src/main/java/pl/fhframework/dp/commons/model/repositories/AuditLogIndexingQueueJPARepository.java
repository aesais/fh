package pl.fhframework.dp.commons.model.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.fhframework.dp.commons.model.entities.AuditLogIndexingQueue;


@Repository
public interface AuditLogIndexingQueueJPARepository extends JpaRepository<AuditLogIndexingQueue, String> {
    Page<AuditLogIndexingQueue> findByIndexed(boolean indexed, Pageable pageable);
    void deleteByIndexedTrue();
}