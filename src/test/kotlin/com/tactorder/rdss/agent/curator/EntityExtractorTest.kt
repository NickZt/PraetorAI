package com.tactorder.rdss.agent.curator

import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.output.Response
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EntityExtractorTest {

    private val chatLanguageModel = mockk<ChatLanguageModel>()
    // We can't easily mock AiServices created via interface proxy without a lot of setup.
    // However, since EntityExtractor wraps it, we can test the parsing logic if we extract it,
    // OR we can trust the integration.
    // For unit testing here, let's refactor EntityExtractor slightly to make it more testable 
    // or test the parsing logic directly if we made it public/internal.

    // BETTER APPROACH FOR UNIT TEST:
    // Create a subclass or standard class that allows injecting the service, 
    // OR just verify the parsing logic which is the custom part.
    // The AiServices part generates the prompt and calls the model.

    // For now, let's write a test that focuses on the parsing logic which is 'private' in the implementation.
    // To make it testable, we'll verify it behaves as expected given a JSON string.

    // Actually, simpler: construct the EntityExtractor and mock the chat model response?
    // LangChain4j AiServices uses the chat model.

    @Test
    fun `should extract entities from valid JSON response`() {
        // Given
        val mockJson = """
            {
                "concepts": ["Distributed Inference", "Fog Computing"],
                "methods": ["Quantization", "Pruning"],
                "datasets": ["CIFAR-10"]
            }
        """.trimIndent()

        every { chatLanguageModel.generate(any<List<dev.langchain4j.data.message.ChatMessage>>()) } returns Response.from(
            dev.langchain4j.data.message.AiMessage.from(mockJson)
        )

        val extractor = EntityExtractor(chatLanguageModel)

        // When
        val result = extractor.extract("some input text")

        // Then
        assertEquals(2, result.concepts.size)
        assertTrue(result.concepts.contains("Distributed Inference"))
        assertTrue(result.concepts.contains("Fog Computing"))

        assertEquals(2, result.methods.size)
        assertTrue(result.methods.contains("Quantization"))

        assertEquals(1, result.datasets.size)
        assertEquals("CIFAR-10", result.datasets[0])
    }

    @Test
    fun `should handle empty lists in JSON`() {
        // Given
        val mockJson = """
            {
                "concepts": [],
                "methods": [],
                "datasets": []
            }
        """.trimIndent()

        every { chatLanguageModel.generate(any<List<dev.langchain4j.data.message.ChatMessage>>()) } returns Response.from(
            dev.langchain4j.data.message.AiMessage.from(mockJson)
        )

        val extractor = EntityExtractor(chatLanguageModel)

        // When
        val result = extractor.extract("text with no entities")

        // Then
        assertTrue(result.concepts.isEmpty())
        assertTrue(result.methods.isEmpty())
        assertTrue(result.datasets.isEmpty())
    }
}
