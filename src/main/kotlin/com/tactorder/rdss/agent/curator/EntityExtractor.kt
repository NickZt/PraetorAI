package com.tactorder.rdss.agent.curator

import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.service.AiServices
import dev.langchain4j.service.UserMessage
import dev.langchain4j.service.V
import org.springframework.stereotype.Component

@Component
class EntityExtractor(
    private val chatLanguageModel: ChatLanguageModel
) {

    interface ExtractorService {
        @UserMessage("""
            Analyze the following text and extract research entities.
            Return a JSON object with the following structure:
            {
                "concepts": ["concept1", "concept2"],
                "methods": ["method1", "method2"],
                "datasets": ["dataset1", "dataset2"]
            }
            
            Text: {{text}}
        """)
        fun extractEntities(@V("text") text: String): String
    }

    private val extractorService: ExtractorService = AiServices.create(ExtractorService::class.java, chatLanguageModel)

    fun extract(text: String): ExtractedEntities {
        val jsonResponse = extractorService.extractEntities(text)
        return parseJson(jsonResponse)
    }

    private fun parseJson(json: String): ExtractedEntities {
        // Simple manual parsing to avoid strict JSON library dependency for this example
        // In a real scenario, use Jackson or Kotlin Serialization
        // This is a robust-enough placeholder for the "clean" JSON expected from LLM
        
        // Remove code blocks if present
        val cleanJson = json.replace("```json", "").replace("```", "").trim()
        
        val concepts = extractList(cleanJson, "concepts")
        val methods = extractList(cleanJson, "methods")
        val datasets = extractList(cleanJson, "datasets")

        return ExtractedEntities(
            concepts = concepts,
            methods = methods,
            datasets = datasets
        )
    }

    private fun extractList(json: String, key: String): List<String> {
        val regex = Regex("\"$key\"\\s*:\\s*\\[(.*?)\\]", RegexOption.DOT_MATCHES_ALL)
        val match = regex.find(json) ?: return emptyList()
        
        val arrayContent = match.groupValues[1]
        if (arrayContent.isBlank()) return emptyList()

        return arrayContent.split(",")
            .map { it.trim().trim('"') }
            .filter { it.isNotBlank() }
    }
}

data class ExtractedEntities(
    val concepts: List<String> = emptyList(),
    val methods: List<String> = emptyList(),
    val datasets: List<String> = emptyList()
)
