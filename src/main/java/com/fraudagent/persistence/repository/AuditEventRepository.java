package com.fraudagent.persistence.repository;

import com.fraudagent.persistence.entity.AuditEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Read + append only. The entity itself rejects updates/deletes.
 */
public interface AuditEventRepository extends JpaRepository<AuditEventEntity, Long> {

    List<AuditEventEntity> findByCaseIdOrderByIdAsc(Long caseId);
}
