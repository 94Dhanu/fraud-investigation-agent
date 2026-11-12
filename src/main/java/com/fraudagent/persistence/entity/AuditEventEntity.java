package com.fraudagent.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Append-only audit event. The entity refuses updates and deletes at the
 * ORM level; the audit trail is immutable history.
 */
@Entity
@Table(name = "audit_event")
public class AuditEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nullable: audit can also record rejected intakes that never became cases. */
    @Column(name = "case_id")
    private Long caseId;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(nullable = false, length = 128)
    private String actor;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(columnDefinition = "TEXT")
    private String details;

    @PrePersist
    void onCreate() {
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }

    @PreUpdate
    void blockUpdate() {
        throw new IllegalStateException("Audit events are append-only and cannot be modified");
    }

    @PreRemove
    void blockDelete() {
        throw new IllegalStateException("Audit events are append-only and cannot be deleted");
    }

    public Long getId() {
        return id;
    }

    public Long getCaseId() {
        return caseId;
    }

    public void setCaseId(Long caseId) {
        this.caseId = caseId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
