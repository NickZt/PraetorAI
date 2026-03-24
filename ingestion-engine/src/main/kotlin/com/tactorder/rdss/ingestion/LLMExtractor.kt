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
        Extract specific entities mentioned in the text:
        1. **Laws/Directives**: ID number (e.g. "104-A") and title.
        2. **Supersession**: Identify if this document REPLACES or SUPERSEDES another (e.g. "supersedes 104-A").
        3. **People**: Full name AND rank/title (e.g. "Commander", "Sergeant") as separate fields.
        4. **Concepts & Sections**: Key topics and numbered paragraphs.

        CRITICAL for 104-B: The text says "This supersedes Directive 104-A". You MUST extract "104-A" as the "supersedes" value.

        JSON structure:
        {
            "concepts": ["topic1"],
            "laws": [{"number": "104-B", "title": "...", "type": "directive", "supersedes": "104-A"}],
            "sections": [{"number": "sec_01", "content": "..."}],
            "people": [{"name": "Jane Doe", "rank": "Commander", "role": "Authorized Officer"}]
        }

        Return ONLY the JSON object.
        
        Strictly use the fields above but fill them with real data from the text. 
        If an entity type is not present, return an empty list: [].
        Do NOT return "person_name" or "law_001" unless they are actually in the text.
        Return ONLY the JSON object.
        
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
            logger.info("LLM Raw Extraction JSON: $rawJson")
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
                    parentDocument.laws.add(law) // Link to Document
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
                        role = pNode.get("role")?.asText(),
                        rank = pNode.get("rank")?.asText()
                    )
                    parentDocument.people.add(person) // Link to Document
                    entities.add(person)
                }
            }

            // 5. Raw Supersedes Info (For post-processing)
            if (rootNode.has("laws")) {
               rootNode.get("laws").forEach { lawNode ->
                   val supersedes = lawNode.get("supersedes")?.asText()
                   if (supersedes != null && supersedes.isNotBlank()) {
                       extracted.metadata["supersedes"] = supersedes
                   }
               }
            }

        } catch (e: Exception) {
            logger.error("Failed to parse LLM JSON: ${extracted.rawJson}", e)
        }

        return entities
    }
}

data class ExtractedEntities(
    val rawJson: String,
    val metadata: MutableMap<String, String> = mutableMapOf()
)
