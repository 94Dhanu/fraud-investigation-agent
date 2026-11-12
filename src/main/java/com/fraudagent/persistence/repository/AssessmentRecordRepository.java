package com.fraudagent.persistence.repository;

import com.fraudagent.persistence.entity.AssessmentRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssessmentRecordRepository extends JpaRepository<AssessmentRecordEntity, Long> {

    List<AssessmentRecordEntity> findByCaseIdOrderByIdDesc(Long caseId);

    Optional<AssessmentRecordEntity> findTopByCaseIdOrderByIdDesc(Long caseId);
}
