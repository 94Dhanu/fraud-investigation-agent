package com.fraudagent.persistence;

import com.fraudagent.persistence.entity.AuditEventEntity;
import com.fraudagent.persistence.repository.AuditEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * POL-515 §3: audit records must never be modified or deleted. The entity
 * enforces this via @PreUpdate/@PreRemove.
 */
@DataJpaTest
class AppendOnlyAuditTest {

    @Autowired
    private AuditEventRepository repository;

    private AuditEventEntity event() {
        AuditEventEntity e = new AuditEventEntity();
        e.setCaseId(1L);
        e.setActor("tester");
        e.setEventType("TEST_EVENT");
        e.setDetails("original");
        return e;
    }

    @Test
    void updatesAreRejected() {
        AuditEventEntity saved = repository.save(event());
        saved.setDetails("tampered");
        assertThatThrownBy(() -> repository.saveAndFlush(saved))
                .hasMessageContaining("append-only");
    }

    @Test
    void deletesAreRejected() {
        AuditEventEntity saved = repository.save(event());
        assertThatThrownBy(() -> {
            repository.delete(saved);
            repository.flush();
        }).hasMessageContaining("append-only");
    }
}
