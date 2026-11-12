package com.fraudagent.persistence.repository;

import com.fraudagent.persistence.entity.EvidenceRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvidenceRecordRepository extends JpaRepository<EvidenceRecordEntity, Long> {

    List<EvidenceRecordEntity> findByCaseIdOrderByIdAsc(Long caseId);
}
