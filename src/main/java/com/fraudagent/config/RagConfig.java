package com.fraudagent.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * In-memory vector store for the banking-policy RAG corpus. The default
 * embedding model is the deterministic local hasher, so the whole pipeline
 * runs with no external calls. Swap in a pgvector-backed store +
 * OpenAI embeddings for production by replacing these beans.
 */
@Configuration
public class RagConfig {

    @Bean
    public VectorStore policyVectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }
}
