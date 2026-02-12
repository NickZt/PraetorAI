package com.tactorder.rdss.config

import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.model.ollama.OllamaChatModel
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
class LangChainConfig {

    @Value("\${ollama.base-url}")
    private lateinit var ollamaBaseUrl: String

    @Value("\${ollama.model}")
    private lateinit var ollamaModelName: String

    @Value("\${ollama.timeout}")
    private lateinit var ollamaTimeout: Duration

    @Bean
    fun chatLanguageModel(): ChatLanguageModel {
        return OllamaChatModel.builder()
            .baseUrl(ollamaBaseUrl)
            .modelName(ollamaModelName)
            .timeout(ollamaTimeout)
            .build()
    }

    @Bean
    fun embeddingModel(): EmbeddingModel {
        // Using AllMiniLmL6V2 for local embeddings as a starting point
        // Can be swapped for BGE-M3 later if needed or configured via properties
        return AllMiniLmL6V2EmbeddingModel()
    }
}
