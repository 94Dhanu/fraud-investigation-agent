package com.fraudagent.api.rest;

import com.fraudagent.api.dto.CaseDetailResponse;
import com.fraudagent.api.dto.CaseIntakeRequest;
import com.fraudagent.domain.model.FraudAssessment;
import com.fraudagent.persistence.entity.AuditEventEntity;
import com.fraudagent.persistence.entity.FraudCaseEntity;
import com.fraudagent.service.AuditService;
import com.fraudagent.service.FraudCaseService;
import com.fraudagent.service.InvestigationService;
import com.fraudagent.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cases")
public class FraudCaseRestController {

    private final FraudCaseService caseService;
    private final InvestigationService investigationService;
    private final ReviewService reviewService;
    private final AuditService auditService;

    public FraudCaseRestController(FraudCaseService caseService,
                                   InvestigationService investigationService,
                                   ReviewService reviewService,
                                   AuditService auditService) {
        this.caseService = caseService;
        this.investigationService = investigationService;
        this.reviewService = reviewService;
        this.auditService = auditService;
    }

    /** Intake + validation + full investigation in one call. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CaseDetailResponse intake(@Valid @RequestBody CaseIntakeRequest request) {
        FraudCaseEntity created = caseService.intakeCase(request);
        return detail(created.getId());
    }

    @GetMapping
    public List<FraudCaseEntity> list() {
        return caseService.listCases();
    }

    @GetMapping("/{caseId}")
    public CaseDetailResponse detail(@PathVariable Long caseId) {
        return new CaseDetailResponse(
                caseService.getCase(caseId),
                investigationService.evidenceFor(caseId),
                investigationService.assessmentsFor(caseId).stream().findFirst().orElse(null),
                investigationService.assessmentsFor(caseId),
                reviewService.decisionsFor(caseId),
                auditService.trailFor(caseId),
                investigationService.transcriptFor(caseId));
    }

    /** Re-run evidence collection + assessment for a non-resolved case. */
    @PostMapping("/{caseId}/reinvestigate")
    public FraudAssessment reinvestigate(@PathVariable Long caseId) {
        return investigationService.investigate(caseId);
    }

    @GetMapping("/{caseId}/audit")
    public List<AuditEventEntity> auditTrail(@PathVariable Long caseId) {
        caseService.getCase(caseId); // 404 if missing
        return auditService.trailFor(caseId);
    }
}
