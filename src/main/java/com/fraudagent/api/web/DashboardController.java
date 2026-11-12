package com.fraudagent.api.web;

import com.fraudagent.ai.PolicyRagService;
import com.fraudagent.api.dto.CaseIntakeRequest;
import com.fraudagent.api.dto.ReviewDecisionRequest;
import com.fraudagent.domain.exception.IntakeValidationException;
import com.fraudagent.domain.exception.InvalidStateTransitionException;
import com.fraudagent.domain.model.CaseStatus;
import com.fraudagent.domain.model.DisputeReason;
import com.fraudagent.domain.model.RecommendedAction;
import com.fraudagent.persistence.entity.FraudCaseEntity;
import com.fraudagent.persistence.entity.ReviewDecisionEntity;
import com.fraudagent.persistence.repository.FraudCaseRepository;
import com.fraudagent.service.AuditService;
import com.fraudagent.service.FraudCaseService;
import com.fraudagent.service.InvestigationService;
import com.fraudagent.service.ReviewService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Controller
public class DashboardController {

    private final FraudCaseService caseService;
    private final InvestigationService investigationService;
    private final ReviewService reviewService;
    private final AuditService auditService;
    private final PolicyRagService policyRagService;
    private final FraudCaseRepository caseRepository;
    private final Validator validator;

    public DashboardController(FraudCaseService caseService,
                               InvestigationService investigationService,
                               ReviewService reviewService,
                               AuditService auditService,
                               PolicyRagService policyRagService,
                               FraudCaseRepository caseRepository,
                               Validator validator) {
        this.caseService = caseService;
        this.investigationService = investigationService;
        this.reviewService = reviewService;
        this.auditService = auditService;
        this.policyRagService = policyRagService;
        this.caseRepository = caseRepository;
        this.validator = validator;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        List<FraudCaseEntity> cases = caseService.listCases();
        model.addAttribute("totalCases", cases.size());
        model.addAttribute("pendingReview", caseRepository.countByStatus(CaseStatus.PENDING_REVIEW));
        model.addAttribute("resolved", caseRepository.countByStatus(CaseStatus.RESOLVED));
        model.addAttribute("awaitingInfo", caseRepository.countByStatus(CaseStatus.MORE_INFO_REQUESTED));
        model.addAttribute("recentCases", cases.stream().limit(8).toList());
        return "dashboard";
    }

    @GetMapping("/cases")
    public String cases(@RequestParam(required = false) String status, Model model) {
        List<FraudCaseEntity> cases = caseService.listCases();
        if (status != null && !status.isBlank()) {
            CaseStatus wanted = CaseStatus.valueOf(status);
            cases = cases.stream().filter(c -> c.getStatus() == wanted).toList();
        }
        model.addAttribute("cases", cases);
        model.addAttribute("statuses", CaseStatus.values());
        model.addAttribute("selectedStatus", status);
        return "cases";
    }

    @GetMapping("/cases/new")
    public String newCaseForm(Model model) {
        model.addAttribute("form", new CaseIntakeForm());
        model.addAttribute("disputeReasons", DisputeReason.values());
        model.addAttribute("channels", List.of("CARD_PRESENT", "ECOMMERCE", "MOTO", "ATM", "RECURRING"));
        return "case-new";
    }

    @PostMapping("/cases")
    public String submitCase(@ModelAttribute("form") CaseIntakeForm form, Model model,
                             RedirectAttributes redirect) {
        CaseIntakeRequest request;
        try {
            request = form.toRequest();
        } catch (Exception e) {
            return intakeError(form, model, List.of("occurredAt must be a valid date-time"));
        }
        Set<ConstraintViolation<CaseIntakeRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            return intakeError(form, model, violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .sorted().toList());
        }
        try {
            FraudCaseEntity created = caseService.intakeCase(request);
            redirect.addFlashAttribute("success",
                    "Case #" + created.getId() + " created and investigated");
            return "redirect:/cases/" + created.getId();
        } catch (IntakeValidationException e) {
            return intakeError(form, model, e.errors());
        }
    }

    private String intakeError(CaseIntakeForm form, Model model, List<String> errors) {
        model.addAttribute("form", form);
        model.addAttribute("errors", errors);
        model.addAttribute("disputeReasons", DisputeReason.values());
        model.addAttribute("channels", List.of("CARD_PRESENT", "ECOMMERCE", "MOTO", "ATM", "RECURRING"));
        return "case-new";
    }

    @GetMapping("/cases/{caseId}")
    public String caseDetail(@PathVariable Long caseId, Model model) {
        model.addAttribute("c", caseService.getCase(caseId));
        model.addAttribute("evidence", investigationService.evidenceFor(caseId));
        var assessments = investigationService.assessmentsFor(caseId);
        model.addAttribute("assessment", assessments.stream().findFirst().orElse(null));
        model.addAttribute("assessmentHistory", assessments);
        model.addAttribute("reviews", reviewService.decisionsFor(caseId));
        model.addAttribute("audit", auditService.trailFor(caseId));
        model.addAttribute("transcript", investigationService.transcriptFor(caseId));
        model.addAttribute("actions", RecommendedAction.values());
        model.addAttribute("decisions", ReviewDecisionEntity.ReviewDecision.values());
        return "case-detail";
    }

    @PostMapping("/cases/{caseId}/reinvestigate")
    public String reinvestigate(@PathVariable Long caseId, RedirectAttributes redirect) {
        try {
            investigationService.investigate(caseId);
            redirect.addFlashAttribute("success", "Re-investigation complete");
        } catch (InvalidStateTransitionException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cases/" + caseId;
    }

    @GetMapping("/review")
    public String reviewQueue(Model model) {
        model.addAttribute("queue", reviewService.reviewQueue());
        return "review-queue";
    }

    @PostMapping("/review/{caseId}")
    public String submitReview(@PathVariable Long caseId,
                               @RequestParam String reviewerId,
                               @RequestParam String decision,
                               @RequestParam(required = false) String finalAction,
                               @RequestParam(required = false) String justification,
                               RedirectAttributes redirect) {
        try {
            ReviewDecisionRequest request = new ReviewDecisionRequest(
                    reviewerId,
                    ReviewDecisionEntity.ReviewDecision.valueOf(decision),
                    finalAction == null || finalAction.isBlank() ? null
                            : RecommendedAction.valueOf(finalAction),
                    justification);
            reviewService.submitDecision(caseId, request);
            redirect.addFlashAttribute("success", "Review decision recorded for case #" + caseId);
            return "redirect:/review";
        } catch (IntakeValidationException e) {
            redirect.addFlashAttribute("error", String.join("; ", e.errors()));
        } catch (InvalidStateTransitionException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cases/" + caseId;
    }

    @GetMapping("/policies")
    public String policies(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("corpus", policyRagService.corpusIndex());
        if (q != null && !q.isBlank()) {
            model.addAttribute("query", q);
            model.addAttribute("hits", policyRagService.retrieve(q, 5));
        }
        return "policies";
    }
}
