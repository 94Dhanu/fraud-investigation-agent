package com.fraudagent.service;

import com.fraudagent.persistence.entity.AuditEventEntity;
import com.fraudagent.persistence.repository.AuditEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Append-only audit trail (POL-515 §3). Events are written in a new
 * transaction so an audit record is never lost if the surrounding business
 * transaction rolls back — and can never be updated or deleted.
 */
@Service
public class AuditService {

    public static final String CASE_INTAKE = "CASE_INTAKE";
    public static final String INTAKE_REJECTED = "INTAKE_REJECTED";
    public static final String INVESTIGATION_STARTED = "INVESTIGATION_STARTED";
    public static final String EVIDENCE_COLLECTED = "EVIDENCE_COLLECTED";
    public static final String ASSESSMENT_RECORDED = "ASSESSMENT_RECORDED";
    public static final String REVIEW_DECISION = "REVIEW_DECISION";
    public static final String CASE_RESOLVED = "CASE_RESOLVED";
    public static final String MORE_INFO_REQUESTED = "MORE_INFO_REQUESTED";

    private final AuditEventRepository auditRepository;

    public AuditService(AuditEventRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditEventEntity record(Long caseId, String actor, String eventType, String details) {
        AuditEventEntity event = new AuditEventEntity();
        event.setCaseId(caseId);
        event.setActor(actor);
        event.setEventType(eventType);
        event.setDetails(details);
        return auditRepository.save(event);
    }

    public List<AuditEventEntity> trailFor(Long caseId) {
        return auditRepository.findByCaseIdOrderByIdAsc(caseId);
    }
}
