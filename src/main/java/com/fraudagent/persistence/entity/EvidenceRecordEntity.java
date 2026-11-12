package com.fraudagent.persistence.entity;

import com.fraudagent.persistence.converter.StringMapJsonConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "evidence_record")
public class EvidenceRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "case_id", nullable = false)
    private Long caseId;

    @Column(name = "tool_name", nullable = false, length = 64)
    private String toolName;

    @Column(nullable = false, length = 1000)
    private String summary;

    @Column(name = "risk_contribution", nullable = false)
    private int riskContribution;

    @Convert(converter = StringMapJsonConverter.class)
    @Column(columnDefinition = "TEXT")
    private Map<String, String> details;

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

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public int getRiskContribution() {
        return riskContribution;
    }

    public void setRiskContribution(int riskContribution) {
        this.riskContribution = riskContribution;
    }

    public Map<String, String> getDetails() {
        return details;
    }

    public void setDetails(Map<String, String> details) {
        this.details = details;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
