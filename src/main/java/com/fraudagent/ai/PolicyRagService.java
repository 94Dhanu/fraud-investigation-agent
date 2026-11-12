package com.fraudagent.ai;

import com.fraudagent.domain.model.CaseFacts;
import com.fraudagent.domain.model.EvidenceItem;
import com.fraudagent.domain.model.PolicyCitation;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Retrieval-Augmented Generation over the banking policy corpus.
 *
 * <p>Each policy markdown file is split into one {@link Document} per
 * {@code ## section}, embedded into the {@link VectorStore}, and retrieved
 * with exact citations (documentId + section + verbatim excerpt). The LLM
 * never invents citations — they are resolved from retrieved documents.
 */
@Service
public class PolicyRagService {

    private static final Logger log = LoggerFactory.getLogger(PolicyRagService.class);

    private final VectorStore vectorStore;
    private final int topK;
    private final List<PolicyCitation> corpusIndex = new ArrayList<>();

    public PolicyRagService(VectorStore policyVectorStore,
                            @Value("${fraud.rag.top-k:4}") int topK) {
        this.vectorStore = policyVectorStore;
        this.topK = topK;
    }

    @PostConstruct
    void indexPolicyCorpus() throws IOException {
        List<Document> chunks = new ArrayList<>();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        for (Resource resource : resolver.getResources("classpath:policies/*.md")) {
            chunks.addAll(chunkPolicy(resource));
        }
        vectorStore.add(chunks);
        log.info("Indexed {} policy sections into the vector store", chunks.size());
    }

    /** One Document per `## section` of each policy file. */
    private List<Document> chunkPolicy(Resource resource) throws IOException {
        String markdown = resource.getContentAsString(StandardCharsets.UTF_8);
        String documentId = extractFrontMatterValue(markdown, "documentId");
        String title = extractFrontMatterValue(markdown, "title");

        String body = markdown.replaceFirst("(?s)^---.*?---", "").trim();
        List<Document> chunks = new ArrayList<>();
        for (String part : body.split("(?m)^## ")) {
            String section = part.trim();
            if (section.isEmpty()) {
                continue;
            }
            int newline = section.indexOf('\n');
            String sectionTitle = newline < 0 ? section : section.substring(0, newline).trim();
            String text = newline < 0 ? "" : section.substring(newline).trim();
            // Retrieval text carries the identifiers so keyword hits on
            // docId/title/section also match; the verbatim excerpt is kept
            // separately for display.
            String retrievable = "POLICY " + documentId + " | " + title + " | SECTION " + sectionTitle
                    + "\n\n" + text;
            chunks.add(new Document(retrievable, Map.of(
                    "documentId", documentId,
                    "title", title,
                    "section", sectionTitle,
                    "excerpt", text)));
            corpusIndex.add(new PolicyCitation(documentId, title, sectionTitle, text, 1.0));
        }
        return chunks;
    }

    private String extractFrontMatterValue(String markdown, String key) {
        for (String line : markdown.split("\n")) {
            if (line.startsWith(key + ":")) {
                return line.substring(key.length() + 1).trim();
            }
        }
        return "UNKNOWN";
    }

    /** Raw similarity search with exact citations. */
    public List<PolicyCitation> retrieve(String query, int k) {
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(k)
                .similarityThreshold(0.0)
                .build();
        List<Document> hits = vectorStore.similaritySearch(request);
        if (hits == null) {
            return List.of();
        }
        List<PolicyCitation> citations = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (Document hit : hits) {
            Map<String, Object> meta = hit.getMetadata();
            String sectionKey = meta.get("documentId") + "::" + meta.get("section");
            if (!seen.add(sectionKey)) {
                continue;
            }
            citations.add(new PolicyCitation(
                    String.valueOf(meta.get("documentId")),
                    String.valueOf(meta.get("title")),
                    String.valueOf(meta.get("section")),
                    String.valueOf(meta.get("excerpt")),
                    hit.getScore() == null ? 0.0 : hit.getScore()));
        }
        return citations;
    }

    public List<PolicyCitation> retrieve(String query) {
        return retrieve(query, topK);
    }

    /**
     * Builds a focused retrieval query from case facts and evidence flags,
     * so citations line up with what actually drove the assessment.
     */
    public List<PolicyCitation> retrieveForCase(CaseFacts facts, List<EvidenceItem> evidence) {
        StringBuilder query = new StringBuilder("unauthorized transaction dispute fraud");
        if (facts.disputeReason() != null) {
            query.append(' ').append(facts.disputeReason().name().replace('_', ' ').toLowerCase());
        }
        if (facts.customerCountry() != null && facts.merchantCountry() != null
                && !facts.customerCountry().equalsIgnoreCase(facts.merchantCountry())) {
            query.append(" cross border foreign transaction");
        }
        Map<String, String> allDetails = new TreeMap<>();
        for (EvidenceItem item : evidence) {
            if (item.details() != null) {
                allDetails.putAll(item.details());
            }
        }
        if ("true".equals(allDetails.get("impossibleTravel"))) {
            query.append(" impossible travel cloned counterfeit card");
        }
        if ("true".equals(allDetails.get("cardTestingPatternDetected"))) {
            query.append(" card testing velocity limits declined attempts");
        }
        if ("true".equals(allDetails.get("onInternalWatchlist"))) {
            query.append(" watchlist merchant escalated senior investigator");
        }
        if ("true".equals(allDetails.get("highRiskCategory"))) {
            query.append(" high risk merchant category chargeback");
        }
        if ("false".equals(allDetails.get("knownDevice"))
                || "true".equals(allDetails.get("emulatorOrProxyDetected"))) {
            query.append(" untrusted device fingerprint");
        }
        if ("true".equals(allDetails.get("accountTakeoverSignal"))) {
            query.append(" account takeover credential contact change");
        }
        try {
            int priorDisputes = Integer.parseInt(allDetails.getOrDefault("priorDisputes", "0"));
            if (priorDisputes >= 3) {
                query.append(" serial disputer first party friendly fraud");
            }
        } catch (NumberFormatException ignored) {
        }
        query.append(" human review escalation audit");
        return retrieve(query.toString());
    }

    /** The whole corpus (for display / audit), document-ordered. */
    public List<PolicyCitation> corpusIndex() {
        return List.copyOf(corpusIndex);
    }
}
