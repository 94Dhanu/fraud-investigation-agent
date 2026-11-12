package com.fraudagent.persistence.entity;

import com.fraudagent.domain.model.CaseStatus;
import com.fraudagent.domain.model.DisputeReason;
import com.fraudagent.domain.model.RecommendedAction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "fraud_case")
public class FraudCaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false, unique = true, length = 64)
    private String transactionId;

    @Column(name = "card_fingerprint", nullable = false, length = 64)
    private String cardFingerprint;

    @Column(name = "account_id", nullable = false, length = 64)
    private String accountId;

    @Column(name = "merchant_id", nullable = false, length = 64)
    private String merchantId;

    @Column(name = "merchant_category", nullable = false, length = 64)
    private String merchantCategory;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "customer_country", nullable = false, length = 2)
    private String customerCountry;

    @Column(name = "merchant_country", nullable = false, length = 2)
    private String merchantCountry;

    @Column(nullable = false, length = 32)
    private String channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "dispute_reason", nullable = false, length = 48)
    private DisputeReason disputeReason;

    @Column(name = "customer_narrative", length = 2000)
    private String customerNarrative;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CaseStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "final_action", length = 48)
    private RecommendedAction finalAction;

    @Column(name = "resolution_notes", length = 2000)
    private String resolutionNotes;

    @Column(name = "assigned_reviewer", length = 128)
    private String assignedReviewer;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getCardFingerprint() {
        return cardFingerprint;
    }

    public void setCardFingerprint(String cardFingerprint) {
        this.cardFingerprint = cardFingerprint;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public String getMerchantCategory() {
        return merchantCategory;
    }

    public void setMerchantCategory(String merchantCategory) {
        this.merchantCategory = merchantCategory;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public String getCustomerCountry() {
        return customerCountry;
    }

    public void setCustomerCountry(String customerCountry) {
        this.customerCountry = customerCountry;
    }

    public String getMerchantCountry() {
        return merchantCountry;
    }

    public void setMerchantCountry(String merchantCountry) {
        this.merchantCountry = merchantCountry;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public DisputeReason getDisputeReason() {
        return disputeReason;
    }

    public void setDisputeReason(DisputeReason disputeReason) {
        this.disputeReason = disputeReason;
    }

    public String getCustomerNarrative() {
        return customerNarrative;
    }

    public void setCustomerNarrative(String customerNarrative) {
        this.customerNarrative = customerNarrative;
    }

    public CaseStatus getStatus() {
        return status;
    }

    public void setStatus(CaseStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public RecommendedAction getFinalAction() {
        return finalAction;
    }

    public void setFinalAction(RecommendedAction finalAction) {
        this.finalAction = finalAction;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public void setResolutionNotes(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
    }

    public String getAssignedReviewer() {
        return assignedReviewer;
    }

    public void setAssignedReviewer(String assignedReviewer) {
        this.assignedReviewer = assignedReviewer;
    }
}
