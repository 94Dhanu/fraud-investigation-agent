package com.fraudagent.service;

import com.fraudagent.api.dto.CaseIntakeRequest;
import com.fraudagent.domain.exception.IntakeValidationException;
import com.fraudagent.domain.model.CaseStatus;
import com.fraudagent.persistence.entity.FraudCaseEntity;
import com.fraudagent.persistence.repository.FraudCaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Case intake and validation. Business rules on top of the bean-validation
 * annotations on {@link CaseIntakeRequest}: transaction ID uniqueness and
 * occurred-at sanity. A rejected intake is still recorded in the append-only
 * audit trail.
 */
@Service
public class FraudCaseService {

    private final FraudCaseRepository caseRepository;
    private final AuditService auditService;
    private final InvestigationService investigationService;

    public FraudCaseService(FraudCaseRepository caseRepository,
                            AuditService auditService,
                            InvestigationService investigationService) {
        this.caseRepository = caseRepository;
        this.auditService = auditService;
        this.investigationService = investigationService;
    }

    /**
     * Validate, persist and immediately investigate a new dispute case.
     *
     * @return the investigated case, now in PENDING_REVIEW
     */
    @Transactional
    public FraudCaseEntity intakeCase(CaseIntakeRequest request) {
        List<String> errors = businessValidate(request);
        if (!errors.isEmpty()) {
            auditService.record(null, "intake-api", AuditService.INTAKE_REJECTED,
                    "Rejected intake for transaction " + request.transactionId()
                            + ": " + String.join("; ", errors));
            throw new IntakeValidationException(errors);
        }

        FraudCaseEntity entity = new FraudCaseEntity();
        entity.setTransactionId(request.transactionId());
        entity.setCardFingerprint(request.cardFingerprint());
        entity.setAccountId(request.accountId());
        entity.setMerchantId(request.merchantId());
        entity.setMerchantCategory(request.merchantCategory());
        entity.setAmount(request.amount());
        entity.setCurrency(request.currency());
        entity.setOccurredAt(request.occurredAt());
        entity.setCustomerCountry(request.customerCountry());
        entity.setMerchantCountry(request.merchantCountry());
        entity.setChannel(request.channel());
        entity.setDisputeReason(request.disputeReason());
        entity.setCustomerNarrative(request.customerNarrative());
        entity.setStatus(CaseStatus.INTAKE);
        FraudCaseEntity saved = caseRepository.save(entity);

        auditService.record(saved.getId(), "intake-api", AuditService.CASE_INTAKE,
                "Case opened for transaction " + saved.getTransactionId()
                        + " — " + saved.getAmount() + " " + saved.getCurrency()
                        + " at merchant " + saved.getMerchantId()
                        + " (" + saved.getDisputeReason() + ")");

        investigationService.investigate(saved.getId());
        return caseRepository.findById(saved.getId()).orElseThrow();
    }

    private List<String> businessValidate(CaseIntakeRequest request) {
        List<String> errors = new ArrayList<>();
        if (request.transactionId() != null
                && caseRepository.existsByTransactionId(request.transactionId())) {
            errors.add("transactionId '" + request.transactionId() + "' already exists");
        }
        if (request.occurredAt() != null && request.occurredAt().isAfter(Instant.now())) {
            errors.add("occurredAt cannot be in the future");
        }
        if (request.customerCountry() != null && request.customerCountry().equals(request.merchantCountry())
                && request.channel() != null && request.channel().equals("ECOMMERCE")) {
            // not an error — informational consistency check only
        }
        return errors;
    }

    public List<FraudCaseEntity> listCases() {
        return caseRepository.findAllByOrderByCreatedAtDesc();
    }

    public FraudCaseEntity getCase(Long caseId) {
        return caseRepository.findById(caseId)
                .orElseThrow(() -> new com.fraudagent.domain.exception.CaseNotFoundException(caseId));
    }
}
