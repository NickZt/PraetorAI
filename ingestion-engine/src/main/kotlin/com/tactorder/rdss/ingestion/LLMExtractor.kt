package com.tactorder.rdss.ingestion

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.tactorder.rdss.domain.*
import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.service.AiServices
import dev.langchain4j.service.UserMessage
import dev.langchain4j.service.V
import org.slf4j.LoggerFactory

interface ExtractionService {
    @UserMessage(
        """
        Analyze the following legal/military text and extract key entities and relationships.
        Return a JSON object with the following structure:
        {
            "concepts": ["concept1", "concept2"],
            "laws": [{"number": "123", "title": "Law Title"}],
            "sections": [{"number": "Art. 1", "content": "summary..."}],
            "people": [{"name": "Name", "role": "Role"}]
        }
        
        Text: {{text}}
    """
    )
    fun extractEntities(@V("text") text: String): String
}


class LLMExtractor(private val chatModel: ChatLanguageModel) {

    private val service: ExtractionService = AiServices.create(ExtractionService::class.java, chatModel)
    private val mapper: ObjectMapper = jacksonObjectMapper()
    private val logger = LoggerFactory.getLogger(LLMExtractor::class.java)

    fun extract(text: String): ExtractedEntities {
        val json = service.extractEntities(text)
        return ExtractedEntities(rawJson = json)
    }

    fun mapToDomainEntities(extracted: ExtractedEntities, parentDocument: Document): List<Any> {
        val entities = mutableListOf<Any>()
        try {
            // Read raw JSON returned by Gateway which might contain markdown formatting
            var rawJson = extracted.rawJson.trim()
            if (rawJson.startsWith("```json")) {
                rawJson = rawJson.removePrefix("```json").removeSuffix("```").trim()
            }

            val rootNode = mapper.readTree(rawJson)

            // 1. Concepts
            if (rootNode.has("concepts")) {
                rootNode.get("concepts").forEach { conceptNode ->
                    val conceptName = conceptNode.asText()
                    val concept = Concept(name = conceptName)
                    // Link to document
                    parentDocument.concepts.add(concept)
                    entities.add(concept)
                }
            }

            // 2. Laws
            if (rootNode.has("laws")) {
                rootNode.get("laws").forEach { lawNode ->
                    val law = Law(
                        type = lawNode.get("type")?.asText() ?: "Unknown Type",
                        number = lawNode.get("number")?.asText() ?: "Unknown Number",
                        title = lawNode.get("title")?.asText() ?: "Unknown Title"
                    )
                    parentDocument.concepts.forEach { law.concepts.add(it) } // Inherit concepts
                    entities.add(law)
                }
            }

            // 3. Sections
            if (rootNode.has("sections")) {
                rootNode.get("sections").forEach { secNode ->
                    val sec = Section(
                        sectionNumber = secNode.get("number")?.asText() ?: "",
                        content = secNode.get("content")?.asText() ?: ""
                    )
                    entities.add(sec)
                }
            }

            // 4. People
            if (rootNode.has("people")) {
                rootNode.get("people").forEach { pNode ->
                    val person = Person(
                        name = pNode.get("name")?.asText() ?: "",
                        role = pNode.get("role")?.asText()
                    )
                    entities.add(person)
                }
            }

        } catch (e: Exception) {
            logger.error("Failed to parse LLM JSON: ${extracted.rawJson}", e)
        }

        return entities
    }
}

data class ExtractedEntities(
    val rawJson: String
    // In real impl, map to domain objects
)
