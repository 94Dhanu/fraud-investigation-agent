package com.fraudagent.domain.port;

import com.fraudagent.domain.model.CaseFacts;
import com.fraudagent.domain.model.EvidenceItem;

/**
 * Framework-neutral evidence-gathering tool. Implementations must be
 * deterministic: identical {@link CaseFacts} must always produce an
 * identical {@link EvidenceItem}.
 */
public interface EvidenceTool {

    /** Stable identifier used in audit records and tool-calling. */
    String toolName();

    /** Human readable description (surfaced to the LLM as the tool description). */
    String description();

    EvidenceItem run(CaseFacts facts);
}
