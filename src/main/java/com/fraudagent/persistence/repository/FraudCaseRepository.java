package com.fraudagent.persistence.repository;

import com.fraudagent.domain.model.CaseStatus;
import com.fraudagent.persistence.entity.FraudCaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FraudCaseRepository extends JpaRepository<FraudCaseEntity, Long> {

    boolean existsByTransactionId(String transactionId);

    Optional<FraudCaseEntity> findByTransactionId(String transactionId);

    List<FraudCaseEntity> findByStatusOrderByCreatedAtAsc(CaseStatus status);

    List<FraudCaseEntity> findAllByOrderByCreatedAtDesc();

    long countByStatus(CaseStatus status);
}
