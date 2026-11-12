package com.fraudagent.persistence.entity;

import com.fraudagent.domain.model.ProcessorUsed;
import com.fraudagent.domain.model.RecommendedAction;
import com.fraudagent.domain.model.RiskLevel;
import com.fraudagent.persistence.converter.PolicyCitationListJsonConverter;
import com.fraudagent.persistence.converter.StringListJsonConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
@Table(name = "assessment_record")
public class AssessmentRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "case_id", nullable = false)
    private Long caseId;

    @Column(name = "risk_score", nullable = false)
    private int riskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 16)
    private RiskLevel riskLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "recommended_action", nullable = false, length = 48)
    private RecommendedAction recommendedAction;

    @Convert(converter = StringListJsonConverter.class)
    @Column(name = "key_factors", columnDefinition = "TEXT")
    private java.util.List<String> keyFactors;

    @Column(nullable = false, length = 4000)
    private String rationale;

    @Convert(converter = PolicyCitationListJsonConverter.class)
    @Column(name = "policy_citations", columnDefinition = "TEXT")
    private java.util.List<com.fraudagent.domain.model.PolicyCitation> policyCitations;

    @Enumerated(EnumType.STRING)
    @Column(name = "processor_used", nullable = false, length = 24)
    private ProcessorUsed processorUsed;

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

    public int getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(int riskScore) {
        this.riskScore = riskScore;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public RecommendedAction getRecommendedAction() {
        return recommendedAction;
    }

    public void setRecommendedAction(RecommendedAction recommendedAction) {
        this.recommendedAction = recommendedAction;
    }

    public java.util.List<String> getKeyFactors() {
        return keyFactors;
    }

    public void setKeyFactors(java.util.List<String> keyFactors) {
        this.keyFactors = keyFactors;
    }

    public String getRationale() {
        return rationale;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale;
    }

    public java.util.List<com.fraudagent.domain.model.PolicyCitation> getPolicyCitations() {
        return policyCitations;
    }

    public void setPolicyCitations(java.util.List<com.fraudagent.domain.model.PolicyCitation> policyCitations) {
        this.policyCitations = policyCitations;
    }

    public ProcessorUsed getProcessorUsed() {
        return processorUsed;
    }

    public void setProcessorUsed(ProcessorUsed processorUsed) {
        this.processorUsed = processorUsed;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
