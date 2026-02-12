package com.tactorder.rdss.ingestion

import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.service.AiServices
import dev.langchain4j.service.UserMessage
import dev.langchain4j.service.V

interface ExtractionService {
    @UserMessage("""
        Analyze the following legal/military text and extract key entities and relationships.
        Return a JSON object with the following structure:
        {
            "concepts": ["concept1", "concept2"],
            "laws": [{"number": "123", "title": "Law Title"}],
            "sections": [{"number": "Art. 1", "content": "summary..."}],
            "people": [{"name": "Name", "role": "Role"}]
        }
        
        Text: {{text}}
    """)
    fun extractEntities(@V("text") text: String): String
}

class LLMExtractor(private val chatModel: ChatLanguageModel) {

    private val service: ExtractionService = AiServices.create(ExtractionService::class.java, chatModel)

    fun extract(text: String): ExtractedEntities {
        val json = service.extractEntities(text)
        // Parse JSON (Manual or Library)
        // For MVP, simplistic parsing or assume clean JSON
        // Ideally use Jackson here.
        
        return ExtractedEntities(rawJson = json)
    }
}

data class ExtractedEntities(
    val rawJson: String
    // In real impl, map to domain objects
)
