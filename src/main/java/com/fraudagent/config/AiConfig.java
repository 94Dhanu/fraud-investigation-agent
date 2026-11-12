package com.fraudagent.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI wiring. The {@link ChatMemory} is always present (harmless in
 * rules mode). The ChatClient itself is built lazily inside
 * {@code SpringAiFraudCaseProcessor} from the auto-configured
 * {@code ChatClient.Builder} — which only exists when a chat model is
 * configured (spring.ai.model.chat=openai + OPENAI_API_KEY). When absent, or
 * when the LLM call fails, the router falls back to the rules engine.
 */
@Configuration
public class AiConfig {

    /**
     * Conversation memory, partitioned per case by conversationId
     * ("case-{id}") — one case's conversation never leaks into another's.
     */
    @Bean
    public ChatMemory fraudCaseChatMemory() {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(40)
                .build();
    }
}
