package com.tactorder.rdss.config

import dev.langchain4j.model.openai.OpenAiChatModel
import dev.langchain4j.model.openai.OpenAiEmbeddingModel
import io.vertx.core.json.JsonObject
import java.time.Duration

object ModelFactory {
    fun createChatModel(config: JsonObject): OpenAiChatModel {
        val llmConfig = config.getJsonObject("llm") ?: JsonObject()
        return OpenAiChatModel.builder()
            .baseUrl(llmConfig.getString("base-url") ?: "http://localhost:8080/v1")
            .apiKey(llmConfig.getString("api.key") ?: System.getenv("LLM_API_KEY") ?: "sk-local")
            .modelName(llmConfig.getJsonObject("chat")?.getString("model") ?: "native-Qwen3-VL-4B-Instruct-Eagle3-MNN")
            .timeout(Duration.ofSeconds(llmConfig.getString("timeout")?.toLong() ?: 180L))
            .build()
    }

    fun createEmbeddingModel(config: JsonObject): OpenAiEmbeddingModel {
        val llmConfig = config.getJsonObject("llm") ?: JsonObject()
        return OpenAiEmbeddingModel.builder()
            .baseUrl(llmConfig.getString("base-url") ?: "http://localhost:8080/v1")
            .apiKey(llmConfig.getString("api.key") ?: "sk-local")
            .modelName(llmConfig.getJsonObject("embedding")?.getString("model") ?: "native-Qwen3-Embedding-4B-MNN")
            .timeout(Duration.ofSeconds(llmConfig.getString("timeout")?.toLong() ?: 180L))
            .build()
    }

}
