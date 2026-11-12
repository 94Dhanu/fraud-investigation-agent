package com.fraudagent.api.web;

import com.fraudagent.api.dto.CaseIntakeRequest;
import com.fraudagent.domain.model.DisputeReason;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Mutable form-backing bean for the Thymeleaf intake form (datetime-local
 * and enum select binding), mapped to the validated {@link CaseIntakeRequest}.
 */
public class CaseIntakeForm {

    private String transactionId;
    private String cardFingerprint;
    private String accountId;
    private String merchantId;
    private String merchantCategory;
    private BigDecimal amount;
    private String currency;
    private String occurredAt; // datetime-local
    private String customerCountry;
    private String merchantCountry;
    private String channel;
    private String disputeReason;
    private String customerNarrative;

    public CaseIntakeRequest toRequest() {
        return new CaseIntakeRequest(
                transactionId,
                cardFingerprint,
                accountId,
                merchantId,
                merchantCategory,
                amount,
                currency == null ? null : currency.toUpperCase(),
                occurredAt == null || occurredAt.isBlank() ? null
                        : LocalDateTime.parse(occurredAt).toInstant(ZoneOffset.UTC),
                customerCountry == null ? null : customerCountry.toUpperCase(),
                merchantCountry == null ? null : merchantCountry.toUpperCase(),
                channel,
                disputeReason == null || disputeReason.isBlank() ? null
                        : DisputeReason.valueOf(disputeReason),
                customerNarrative);
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

    public String getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(String occurredAt) {
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

    public String getDisputeReason() {
        return disputeReason;
    }

    public void setDisputeReason(String disputeReason) {
        this.disputeReason = disputeReason;
    }

    public String getCustomerNarrative() {
        return customerNarrative;
    }

    public void setCustomerNarrative(String customerNarrative) {
        this.customerNarrative = customerNarrative;
    }
}
