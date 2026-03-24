package com.tactorder.rdss.ingestion

import com.tactorder.rdss.domain.Document
import dev.langchain4j.model.openai.OpenAiChatModel
import org.junit.jupiter.api.Test
import java.time.Duration

class LLMExtractorTest {

    @Test
    fun testExtraction() {
        val chatModel = OpenAiChatModel.builder()
            .baseUrl("http://localhost:8080/v1")
            .apiKey("sk-local")
            .modelName("native-Qwen3-VL-4B-Instruct-Eagle3-MNN")
            .timeout(Duration.ofSeconds(180))
            .build()

        val extractor = LLMExtractor(chatModel)
        val text = """
            HEADQUARTERS FIELD DIRECTIVE 104-B
            Date: 2024-05-10
            Subject: Autonomous Drone Deployment Protocol
            Protocol: All Alpha-Class Drones must now maintain a maximum altitude of 400 feet.
            Authorized Officer: Commander Jane Doe.
        """.trimIndent()

        println("--- START TEST EXTRACTION ---")
        val extracted = extractor.extract(text)
        println("RAW JSON: ${extracted.rawJson}")

        val doc = Document(title = "Test Doc")
        val entities = extractor.mapToDomainEntities(extracted, doc)
        println("MAPPED ENTITIES: ${entities.map { it::class.simpleName }}")
        println("DOCUMENT LAWS: ${doc.laws.map { it.number }}")
        println("--- END TEST EXTRACTION ---")
    }
}
