package com.fraudagent.domain.model;

/**
 * An exact citation into the bank's policy corpus produced by RAG retrieval.
 */
public record PolicyCitation(
        String documentId,
        String documentTitle,
        String section,
        String excerpt,
        double relevanceScore
) {
}
