package com.fraudagent.domain.port;

import com.fraudagent.domain.model.CaseFacts;
import com.fraudagent.domain.model.EvidenceItem;
import com.fraudagent.domain.model.FraudAssessment;

import java.util.List;

/**
 * Framework-neutral fraud case processor. Implementations:
 * <ul>
 *   <li>{@code RulesFraudCaseProcessor} — deterministic scoring engine (default + fallback)</li>
 *   <li>{@code SpringAiFraudCaseProcessor} — LLM agent with tool calling and RAG</li>
 * </ul>
 */
public interface FraudCaseProcessor {

    /**
     * Produce a structured risk recommendation for a case.
     *
     * @param facts    immutable case facts from intake
     * @param evidence evidence already collected by the deterministic tools
     */
    FraudAssessment assess(CaseFacts facts, List<EvidenceItem> evidence);
}
