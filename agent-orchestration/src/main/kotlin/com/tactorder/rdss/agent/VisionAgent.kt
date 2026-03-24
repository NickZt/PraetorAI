package com.tactorder.rdss.agent

import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.ImageContent
import dev.langchain4j.data.message.TextContent
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.openai.OpenAiChatModel
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory
import java.time.Duration

class VisionAgent(private val config: JsonObject) {
    private val logger = LoggerFactory.getLogger(VisionAgent::class.java)

    private val llm: ChatLanguageModel = OpenAiChatModel.builder()
        .baseUrl(config.getJsonObject("llm")?.getString("base-url") ?: "http://localhost:8080/v1")
        .modelName(config.getJsonObject("llm")?.getJsonObject("chat")?.getString("model") ?: "native-Qwen3-VL-4B-Instruct-Eagle3-MNN")
        .apiKey(config.getJsonObject("llm")?.getString("api.key") ?: System.getenv("LLM_API_KEY") ?: "sk-local")
        .timeout(Duration.ofSeconds(config.getJsonObject("llm")?.getString("timeout")?.toLong() ?: 180L))
        .build()

    /**
     * Performs OCR and Captioning on an image.
     * Expects payload: { "image_path": "...", "task": "caption|ocr" }
     */
    fun analyzeImage(payload: JsonObject): JsonObject {
        val imagePath = payload.getString("image_path")
        val task = payload.getString("task", "caption")
        
        logger.info("VisionAgent: Analyzing image $imagePath (Task: $task)")

        val prompt = when (task) {
            "ocr" -> "Perform OCR on this image. Return only the extracted text."
            else -> "Describe this image in detail, focusing on tactical elements, personnel, and equipment."
        }

        try {
            // Load image as base64
            val fileBytes = java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(imagePath))
            val base64Image = java.util.Base64.getEncoder().encodeToString(fileBytes)
            val mimeType = when {
                imagePath.endsWith(".png", ignoreCase = true) -> "image/png"
                else -> "image/jpeg"
            }

            val userMessage = UserMessage.from(
                TextContent.from(prompt),
                ImageContent.from(base64Image, mimeType)
            )

            val response = llm.generate(userMessage)
            val text = response.content().text()

            return JsonObject()
                .put("status", "success")
                .put("analysis", text)
        } catch (e: Exception) {
            logger.error("VisionAgent failed: ${e.message}")
            return JsonObject()
                .put("status", "error")
                .put("message", e.message)
        }
    }
}
