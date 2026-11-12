package com.fraudagent.service;

import com.fraudagent.api.dto.ReviewDecisionRequest;
import com.fraudagent.domain.exception.InvalidStateTransitionException;
import com.fraudagent.domain.exception.IntakeValidationException;
import com.fraudagent.domain.model.CaseStatus;
import com.fraudagent.domain.model.RecommendedAction;
import com.fraudagent.persistence.entity.AssessmentRecordEntity;
import com.fraudagent.persistence.entity.FraudCaseEntity;
import com.fraudagent.persistence.entity.ReviewDecisionEntity;
import com.fraudagent.persistence.repository.AssessmentRecordRepository;
import com.fraudagent.persistence.repository.FraudCaseRepository;
import com.fraudagent.persistence.repository.ReviewDecisionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Human review and approval workflow (POL-515 §1). The agent recommendation
 * is advisory — a human reviewer must approve it, override it with a
 * justification, or request more information before a case can resolve.
 */
@Service
public class ReviewService {

    private final FraudCaseRepository caseRepository;
    private final ReviewDecisionRepository reviewRepository;
    private final AssessmentRecordRepository assessmentRepository;
    private final AuditService auditService;
    private final CaseFactsProvider factsProvider;

    public ReviewService(FraudCaseRepository caseRepository,
                         ReviewDecisionRepository reviewRepository,
                         AssessmentRecordRepository assessmentRepository,
                         AuditService auditService,
                         CaseFactsProvider factsProvider) {
        this.caseRepository = caseRepository;
        this.reviewRepository = reviewRepository;
        this.assessmentRepository = assessmentRepository;
        this.auditService = auditService;
        this.factsProvider = factsProvider;
    }

    @Transactional
    public ReviewDecisionEntity submitDecision(Long caseId, ReviewDecisionRequest request) {
        FraudCaseEntity entity = factsProvider.load(caseId);

        if (entity.getStatus() != CaseStatus.PENDING_REVIEW
                && entity.getStatus() != CaseStatus.MORE_INFO_REQUESTED) {
            throw new InvalidStateTransitionException(
                    "Case " + caseId + " is in status " + entity.getStatus()
                            + " — only PENDING_REVIEW or MORE_INFO_REQUESTED cases can be decided");
        }

        List<String> errors = validate(request);
        if (!errors.isEmpty()) {
            throw new IntakeValidationException(errors);
        }

        RecommendedAction finalAction = switch (request.decision()) {
            case APPROVE_RECOMMENDATION -> latestRecommendation(caseId);
            case OVERRIDE_RECOMMENDATION -> request.finalAction();
            case REQUEST_MORE_INFORMATION -> null;
        };

        ReviewDecisionEntity decision = new ReviewDecisionEntity();
        decision.setCaseId(caseId);
        decision.setReviewerId(request.reviewerId());
        decision.setDecision(request.decision());
        decision.setFinalAction(finalAction);
        decision.setJustification(request.justification());
        ReviewDecisionEntity saved = reviewRepository.save(decision);

        switch (request.decision()) {
            case APPROVE_RECOMMENDATION, OVERRIDE_RECOMMENDATION -> {
                entity.setStatus(CaseStatus.RESOLVED);
                entity.setFinalAction(finalAction);
                entity.setResolutionNotes(request.justification());
                entity.setAssignedReviewer(request.reviewerId());
                entity.setResolvedAt(Instant.now());
            }
            case REQUEST_MORE_INFORMATION -> entity.setStatus(CaseStatus.MORE_INFO_REQUESTED);
        }
        caseRepository.save(entity);

        auditService.record(caseId, request.reviewerId(), AuditService.REVIEW_DECISION,
                request.decision() + (finalAction != null ? " → " + finalAction : "")
                        + (request.justification() != null ? " — " + request.justification() : ""));
        if (entity.getStatus() == CaseStatus.RESOLVED) {
            auditService.record(caseId, request.reviewerId(), AuditService.CASE_RESOLVED,
                    "Case resolved with final action " + finalAction);
        } else {
            auditService.record(caseId, request.reviewerId(), AuditService.MORE_INFO_REQUESTED,
                    "Reviewer requested additional information");
        }
        return saved;
    }

    private List<String> validate(ReviewDecisionRequest request) {
        List<String> errors = new ArrayList<>();
        if (request.decision() == ReviewDecisionEntity.ReviewDecision.OVERRIDE_RECOMMENDATION) {
            if (request.finalAction() == null) {
                errors.add("finalAction is required when overriding the recommendation");
            }
            if (request.justification() == null || request.justification().isBlank()) {
                errors.add("justification is required when overriding the recommendation");
            }
        }
        if (request.decision() == ReviewDecisionEntity.ReviewDecision.REQUEST_MORE_INFORMATION
                && (request.justification() == null || request.justification().isBlank())) {
            errors.add("justification is required when requesting more information");
        }
        return errors;
    }

    private RecommendedAction latestRecommendation(Long caseId) {
        return assessmentRepository.findTopByCaseIdOrderByIdDesc(caseId)
                .map(AssessmentRecordEntity::getRecommendedAction)
                .orElseThrow(() -> new InvalidStateTransitionException(
                        "Case " + caseId + " has no assessment to approve"));
    }

    @Transactional(readOnly = true)
    public List<FraudCaseEntity> reviewQueue() {
        return caseRepository.findByStatusOrderByCreatedAtAsc(CaseStatus.PENDING_REVIEW);
    }

    @Transactional(readOnly = true)
    public List<ReviewDecisionEntity> decisionsFor(Long caseId) {
        return reviewRepository.findByCaseIdOrderByIdDesc(caseId);
    }
}
