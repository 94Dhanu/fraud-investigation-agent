package com.fraudagent.persistence.entity;

import com.fraudagent.domain.model.RecommendedAction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "review_decision")
public class ReviewDecisionEntity {

    public enum ReviewDecision {
        APPROVE_RECOMMENDATION,
        OVERRIDE_RECOMMENDATION,
        REQUEST_MORE_INFORMATION
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "case_id", nullable = false)
    private Long caseId;

    @Column(name = "reviewer_id", nullable = false, length = 128)
    private String reviewerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 48)
    private ReviewDecision decision;

    @Enumerated(EnumType.STRING)
    @Column(name = "final_action", length = 48)
    private RecommendedAction finalAction;

    @Column(length = 2000)
    private String justification;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
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

    public String getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(String reviewerId) {
        this.reviewerId = reviewerId;
    }

    public ReviewDecision getDecision() {
        return decision;
    }

    public void setDecision(ReviewDecision decision) {
        this.decision = decision;
    }

    public RecommendedAction getFinalAction() {
        return finalAction;
    }

    public void setFinalAction(RecommendedAction finalAction) {
        this.finalAction = finalAction;
    }

    public String getJustification() {
        return justification;
    }

    public void setJustification(String justification) {
        this.justification = justification;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
