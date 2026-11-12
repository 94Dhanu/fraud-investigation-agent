package com.fraudagent.persistence.repository;

import com.fraudagent.persistence.entity.ReviewDecisionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewDecisionRepository extends JpaRepository<ReviewDecisionEntity, Long> {

    List<ReviewDecisionEntity> findByCaseIdOrderByIdDesc(Long caseId);
}
