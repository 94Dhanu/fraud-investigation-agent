package com.fraudagent.service;

import com.fraudagent.domain.exception.CaseNotFoundException;
import com.fraudagent.domain.model.CaseFacts;
import com.fraudagent.persistence.entity.FraudCaseEntity;
import com.fraudagent.persistence.repository.FraudCaseRepository;
import org.springframework.stereotype.Component;

/**
 * Maps a persisted case to the framework-neutral {@link CaseFacts} view.
 * Used by the investigation service and by the LLM tool callbacks, which
 * receive only a caseId from the model.
 */
@Component
public class CaseFactsProvider {

    private final FraudCaseRepository caseRepository;

    public CaseFactsProvider(FraudCaseRepository caseRepository) {
        this.caseRepository = caseRepository;
    }

    public CaseFacts factsFor(Long caseId) {
        return toFacts(load(caseId));
    }

    public FraudCaseEntity load(Long caseId) {
        return caseRepository.findById(caseId)
                .orElseThrow(() -> new CaseNotFoundException(caseId));
    }

    public static CaseFacts toFacts(FraudCaseEntity e) {
        return new CaseFacts(
                e.getId(),
                e.getTransactionId(),
                e.getCardFingerprint(),
                e.getAccountId(),
                e.getMerchantId(),
                e.getMerchantCategory(),
                e.getAmount(),
                e.getCurrency(),
                e.getOccurredAt(),
                e.getCustomerCountry(),
                e.getMerchantCountry(),
                e.getChannel(),
                e.getDisputeReason(),
                e.getCustomerNarrative());
    }
}
