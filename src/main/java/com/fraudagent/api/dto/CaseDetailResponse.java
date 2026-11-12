package com.fraudagent.api.dto;

import com.fraudagent.persistence.entity.AssessmentRecordEntity;
import com.fraudagent.persistence.entity.AuditEventEntity;
import com.fraudagent.persistence.entity.ChatTranscriptEntity;
import com.fraudagent.persistence.entity.EvidenceRecordEntity;
import com.fraudagent.persistence.entity.FraudCaseEntity;
import com.fraudagent.persistence.entity.ReviewDecisionEntity;

import java.util.List;

/**
 * Full case dossier: facts, evidence, latest + historical assessments,
 * review decisions, agent transcript and the append-only audit trail.
 */
public record CaseDetailResponse(
        FraudCaseEntity fraudCase,
        List<EvidenceRecordEntity> evidence,
        AssessmentRecordEntity latestAssessment,
        List<AssessmentRecordEntity> assessmentHistory,
        List<ReviewDecisionEntity> reviewDecisions,
        List<AuditEventEntity> auditTrail,
        List<ChatTranscriptEntity> agentTranscript
) {
}
