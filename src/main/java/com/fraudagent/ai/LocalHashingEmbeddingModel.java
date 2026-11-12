package com.fraudagent.ai;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Deterministic, dependency-free {@link EmbeddingModel}: a hashed
 * bag-of-words vectoriser with L2 normalisation. It lets the policy RAG
 * pipeline run fully offline — identical text always produces an identical
 * vector, and semantically related texts (shared vocabulary) produce real,
 * orderable cosine scores.
 *
 * <p>In a deployment with an embeddings API available, this bean can simply be
 * replaced by e.g. an OpenAI-backed {@code EmbeddingModel}; the rest of the
 * RAG pipeline is unchanged.
 */
@Primary
@Component
public class LocalHashingEmbeddingModel implements EmbeddingModel {

    public static final int DIMENSIONS = 256;

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<Embedding> results = new ArrayList<>();
        int index = 0;
        for (String text : request.getInstructions()) {
            results.add(new Embedding(vectorize(text), index++));
        }
        return new EmbeddingResponse(results);
    }

    @Override
    public float[] embed(Document document) {
        return vectorize(document.getText());
    }

    @Override
    public int dimensions() {
        return DIMENSIONS;
    }

    private float[] vectorize(String text) {
        float[] vector = new float[DIMENSIONS];
        if (text != null) {
            for (String token : text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
                if (token.length() < 2) {
                    continue;
                }
                vector[Math.floorMod(token.hashCode(), DIMENSIONS)] += 1.0f;
                // add the word stem too, to soften inflection differences
                if (token.length() > 5) {
                    vector[Math.floorMod(token.substring(0, token.length() - 2).hashCode(), DIMENSIONS)] += 0.5f;
                }
            }
        }
        double norm = 0;
        for (float v : vector) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] = (float) (vector[i] / norm);
            }
        }
        return vector;
    }
}
