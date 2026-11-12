package com.fraudagent.api.rest;

import com.fraudagent.ai.PolicyRagService;
import com.fraudagent.domain.model.PolicyCitation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/policies")
public class PolicyRestController {

    private final PolicyRagService policyRagService;

    public PolicyRestController(PolicyRagService policyRagService) {
        this.policyRagService = policyRagService;
    }

    /** Semantic search over the policy corpus — returns exact citations. */
    @GetMapping("/search")
    public List<PolicyCitation> search(@RequestParam String q,
                                       @RequestParam(defaultValue = "4") int k) {
        return policyRagService.retrieve(q, Math.min(Math.max(k, 1), 10));
    }

    @GetMapping
    public List<PolicyCitation> corpus() {
        return policyRagService.corpusIndex();
    }
}
