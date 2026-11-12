package com.fraudagent.persistence.repository;

import com.fraudagent.persistence.entity.ChatTranscriptEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatTranscriptRepository extends JpaRepository<ChatTranscriptEntity, Long> {

    List<ChatTranscriptEntity> findByCaseIdOrderByIdAsc(Long caseId);
}
