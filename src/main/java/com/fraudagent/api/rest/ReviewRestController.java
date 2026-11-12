package com.fraudagent.api.rest;

import com.fraudagent.api.dto.ReviewDecisionRequest;
import com.fraudagent.persistence.entity.FraudCaseEntity;
import com.fraudagent.persistence.entity.ReviewDecisionEntity;
import com.fraudagent.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/review")
public class ReviewRestController {

    private final ReviewService reviewService;

    public ReviewRestController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/queue")
    public List<FraudCaseEntity> queue() {
        return reviewService.reviewQueue();
    }

    @PostMapping("/{caseId}/decision")
    public ReviewDecisionEntity decide(@PathVariable Long caseId,
                                       @Valid @RequestBody ReviewDecisionRequest request) {
        return reviewService.submitDecision(caseId, request);
    }
}
