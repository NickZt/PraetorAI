package com.tactorder.rdss.config

import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.model.openai.OpenAiChatModel
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
class LangChainConfig {

    @Value("\${llm.base-url}")
    private lateinit var llmBaseUrl: String

    @Value("\${llm.model}")
    private lateinit var llmModelName: String

    @Value("\${llm.timeout}")
    private lateinit var llmTimeout: Duration

    @Bean
    fun chatLanguageModel(): ChatLanguageModel {
        return OpenAiChatModel.builder()
            .baseUrl(llmBaseUrl)
            .modelName(llmModelName)
            .apiKey("demo") // Dummy API key usually required by OpenAI client
            .timeout(llmTimeout)
            .build()
    }

    @Bean
    fun embeddingModel(): EmbeddingModel {
        // Using AllMiniLmL6V2 for local embeddings as a starting point
        // Can be swapped for BGE-M3 later if needed or configured via properties
        return AllMiniLmL6V2EmbeddingModel()
    }
}
