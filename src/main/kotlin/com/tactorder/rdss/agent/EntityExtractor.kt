package com.tactorder.rdss.agent

import com.tactorder.rdss.config.LangChainConfig
import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.service.V
import org.springframework.stereotype.Component

@Component
class EntityExtractor(
    private val chatModel: ChatLanguageModel
) {
    
    data class ExtractedEntities(
        val concepts: List<ExtractedConcept>,
        val methods: List<ExtractedMethod>,
        val datasets: List<ExtractedDataset>,
        val researchAreas: List<ExtractedResearchArea>
    )
    
    data class ExtractedConcept(
        val name: String,
        val description: String,
        val confidence: Double,
        val context: String
    )
    
    data class ExtractedMethod(
        val name: String,
        val description: String,
        val confidence: Double,
        val context: String
    )
    
    data class ExtractedDataset(
        val name: String,
        val description: String,
        val confidence: Double,
        val context: String
    )
    
    data class ExtractedResearchArea(
        val name: String,
        val description: String,
        val confidence: Double,
        val context: String
    )
    
    /**
     * Extract entities from document text using LLM
     */
    fun extractEntities(text: String, documentTitle: String? = null): ExtractedEntities {
        val prompt = buildExtractionPrompt(text, documentTitle)
        val response = chatModel.generate(prompt)
        
        return parseExtractionResponse(response.content().text())
    }
    
    private fun buildExtractionPrompt(text: String, documentTitle: String? = null): String {
        val titleContext = documentTitle?.let { "Document Title: $it\n\n" } ?: ""
        
        return """
            $titleContext
            You are an expert research assistant specializing in academic text analysis. 
            Extract the following entities from the provided research text:
            
            1. **Concepts**: Key research concepts, theories, frameworks, or ideas
            2. **Methods**: Research methodologies, algorithms, techniques, or approaches
            3. **Datasets**: Named datasets, corpora, or data sources
            4. **Research Areas**: Academic fields, domains, or sub-disciplines
            
            For each entity, provide:
            - Name: The exact entity name as it appears or should be standardized
            - Description: Brief explanation of what it is
            - Confidence: Your confidence level (0.0-1.0) that this is a valid entity
            - Context: The surrounding text that helped identify this entity
            
            Format your response as JSON:
            ```json
            {
              "concepts": [
                {
                  "name": "Entity Name",
                  "description": "Brief description",
                  "confidence": 0.9,
                  "context": "Surrounding text..."
                }
              ],
              "methods": [...],
              "datasets": [...],
              "researchAreas": [...]
            }
            ```
            
            Text to analyze:
            ---
            ${text.take(8000)} // Limit to prevent token overflow
            ---
            
            Focus on entities that would be valuable for a research knowledge graph. 
            Be conservative with confidence scores - only include entities you're reasonably certain about.
        """.trimIndent()
    }
    
    private fun parseExtractionResponse(response: String): ExtractedEntities {
        return try {
            // Extract JSON from response
            val jsonMatch = Regex("```json\\s*(\\{.*?\\})\\s*```", RegexOption.DOT_MATCHES_ALL)
                .find(response)
                ?.groupValues?.get(1)
                ?: response.trim()
            
            // Parse JSON (simplified approach - in production, use proper JSON parser)
            parseJsonToEntities(jsonMatch)
        } catch (e: Exception) {
            // Fallback to empty entities if parsing fails
            ExtractedEntities(emptyList(), emptyList(), emptyList(), emptyList())
        }
    }
    
    private fun parseJsonToEntities(json: String): ExtractedEntities {
        // This is a simplified JSON parser - in production, use Jackson or similar
        val concepts = parseEntityArray<ExtractedConcept>(json, "concepts")
        val methods = parseEntityArray<ExtractedMethod>(json, "methods")
        val datasets = parseEntityArray<ExtractedDataset>(json, "datasets")
        val researchAreas = parseEntityArray<ExtractedResearchArea>(json, "researchAreas")
        
        return ExtractedEntities(concepts, methods, datasets, researchAreas)
    }
    
    private inline fun <reified T> parseEntityArray(json: String, key: String): List<T> {
        // Simplified parsing - in production, use proper JSON deserialization
        return try {
            val pattern = Regex("\"$key\"\\s*:\\s*\\[(.*?)\\]", RegexOption.DOT_MATCHES_ALL)
            val match = pattern.find(json) ?: return emptyList()
            
            val arrayContent = match.groupValues[1]
            // This would need proper JSON parsing in production
            emptyList() // Placeholder
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Extract entities with specific focus on concepts
     */
    fun extractConcepts(text: String): List<ExtractedConcept> {
        val entities = extractEntities(text)
        return entities.concepts.filter { it.confidence >= 0.7 }
    }
    
    /**
     * Extract entities with specific focus on methods
     */
    fun extractMethods(text: String): List<ExtractedMethod> {
        val entities = extractEntities(text)
        return entities.methods.filter { it.confidence >= 0.7 }
    }
    
    /**
     * Extract entities with specific focus on research areas
     */
    fun extractResearchAreas(text: String): List<ExtractedResearchArea> {
        val entities = extractEntities(text)
        return entities.researchAreas.filter { it.confidence >= 0.6 }
    }
    
    /**
     * Validate and clean extracted entities
     */
    fun validateEntities(entities: ExtractedEntities): ExtractedEntities {
        return ExtractedEntities(
            concepts = entities.concepts.filter { isValidEntity(it.name, it.confidence) },
            methods = entities.methods.filter { isValidEntity(it.name, it.confidence) },
            datasets = entities.datasets.filter { isValidEntity(it.name, it.confidence) },
            researchAreas = entities.researchAreas.filter { isValidEntity(it.name, it.confidence) }
        )
    }
    
    private fun isValidEntity(name: String, confidence: Double): Boolean {
        return name.isNotBlank() && 
               name.length > 2 && 
               confidence >= 0.5 &&
               !name.lowercase().contains("example") &&
               !name.lowercase().contains("etc")
    }
}
