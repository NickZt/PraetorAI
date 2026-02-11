package com.tactorder.rdss.agent

import com.tactorder.rdss.domain.*
import com.tactorder.rdss.repository.*
import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.embedding.EmbeddingModel
import io.weaviate.client.WeaviateClient
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.math.sqrt

@Service
@Transactional
class ConnectorAgent(
    private val chatModel: ChatLanguageModel,
    private val embeddingModel: EmbeddingModel,
    private val weaviateClient: WeaviateClient,
    private val conceptRepository: ConceptRepository,
    private val sourceDocumentRepository: SourceDocumentRepository
) {
    
    private val logger = KotlinLogging.logger {}
    
    data class Connection(
        val fromEntity: String,
        val toEntity: String,
        val relationshipType: String,
        val confidence: Double,
        val reasoning: String,
        val evidence: List<String>
    )
    
    data class SimilarityResult(
        val entityId: String,
        val entityType: String,
        val similarity: Double,
        val explanation: String
    )
    
    /**
     * Find semantic similarities between concepts using vector search
     */
    suspend fun findSemanticSimilarities(
        conceptId: String,
        threshold: Double = 0.8,
        maxResults: Int = 20
    ): List<SimilarityResult> {
        
        val concept = conceptRepository.findById(conceptId).orElseThrow()
        logger.info { "Finding semantic similarities for concept: ${concept.name}" }
        
        // Generate embedding for the concept
        val conceptEmbedding = embeddingModel.embed(concept.description).content()
        
        // Search for similar entities in Weaviate
        val similarEntities = searchSimilarEntities(conceptEmbedding, threshold, maxResults)
        
        return similarEntities.map { entity ->
            SimilarityResult(
                entityId = entity.id,
                entityType = entity.type,
                similarity = entity.similarity,
                explanation = generateSimilarityExplanation(concept, entity)
            )
        }
    }
    
    /**
     * Find indirect connections between concepts using graph traversal
     */
    fun findIndirectConnections(
        conceptId1: String,
        conceptId2: String,
        maxDepth: Int = 3
    ): List<List<String>> {
        logger.info { "Finding indirect connections between $conceptId1 and $conceptId2" }
        
        val paths = mutableListOf<List<String>>()
        
        // Use Neo4j to find paths between concepts
        val query = """
            MATCH path = shortestPath(
                (c1:Concept)-[*1..$maxDepth]-(c2:Concept)
            )
            WHERE c1.id = '$conceptId1' AND c2.id = '$conceptId2'
            RETURN [node in nodes(path) | node.id] as path
        """
        
        // Execute query and collect paths (simplified - would need proper Neo4j template)
        // val result = neo4jTemplate.query(query, emptyMap<String, Any>())
        // result.forEach { row ->
        //     paths.add(row["path"] as List<String>)
        // }
        
        return paths
    }
    
    /**
     * Detect bibliographic coupling between documents
     */
    fun findBibliographicCoupling(documentId: String, threshold: Int = 2): List<String> {
        logger.info { "Finding bibliographic coupling for document: $documentId" }
        
        val coupledDocuments = mutableListOf<String>()
        
        // Find documents that cite the same sources
        val query = """
            MATCH (d1:SourceDocument)-[:CITES]->(common:SourceDocument)<-[:CITES]-(d2:SourceDocument)
            WHERE d1.id = '$documentId' AND d1.id <> d2.id
            WITH d2, COUNT(common) as commonCitations
            WHERE commonCitations >= $threshold
            RETURN d2.id as documentId, commonCitations
            ORDER BY commonCitations DESC
        """
        
        // Execute query and collect results
        // val result = neo4jTemplate.query(query, emptyMap<String, Any>())
        // result.forEach { row ->
        //     coupledDocuments.add(row["documentId"] as String)
        // }
        
        return coupledDocuments
    }
    
    /**
     * Use LLM to reason about potential connections
     */
    suspend fun reasonAboutConnections(
        entityId1: String,
        entityType1: String,
        entityId2: String,
        entityType2: String
    ): List<Connection> {
        
        logger.info { "LLM reasoning about connections between $entityId1 and $entityId2" }
        
        val entity1 = getEntity(entityId1, entityType1)
        val entity2 = getEntity(entityId2, entityType2)
        
        val prompt = buildConnectionReasoningPrompt(entity1, entity2)
        val response = chatModel.generate(prompt)
        
        return parseConnectionResponse(response.content().text(), entityId1, entityId2)
    }
    
    private fun buildConnectionReasoningPrompt(entity1: Any, entity2: Any): String {
        return """
            You are an expert research analyst specializing in identifying relationships between academic concepts and documents.
            
            Analyze the following two entities and determine if there are meaningful connections between them:
            
            Entity 1:
            ${formatEntity(entity1)}
            
            Entity 2:
            ${formatEntity(entity2)}
            
            Consider the following types of connections:
            1. **Conceptual Relationships**: One concept builds on, contradicts, or extends the other
            2. **Methodological Connections**: Shared methods, techniques, or approaches
            3. **Citation Networks**: Direct or indirect citation relationships
            4. **Temporal Relationships**: Chronological development or evolution
            5. **Domain Overlap**: Shared research areas or applications
            
            For each potential connection, provide:
            - Relationship type (from the list above)
            - Confidence level (0.0-1.0)
            - Reasoning for why this connection exists
            - Evidence supporting the connection
            
            Format your response as JSON:
            ```json
            {
              "connections": [
                {
                  "relationshipType": "Conceptual Relationships",
                  "confidence": 0.8,
                  "reasoning": "Both concepts address similar problems...",
                  "evidence": ["Shared terminology", "Common methodologies"]
                }
              ]
            }
            ```
            
            Be conservative with confidence scores - only suggest connections you're reasonably confident about.
        """.trimIndent()
    }
    
    private fun formatEntity(entity: Any): String {
        return when (entity) {
            is Concept -> """
                Type: Concept
                Name: ${entity.name}
                Description: ${entity.description}
                Maturity: ${entity.maturity}
                Tags: ${entity.tags.joinToString(", ")}
            """.trimIndent()
            
            is SourceDocument -> """
                Type: Document
                Title: ${entity.title}
                Authors: ${entity.authors.joinToString(", ")}
                Year: ${entity.year ?: "Unknown"}
                Abstract: ${entity.abstract ?: "No abstract available"}
            """.trimIndent()
            
            else -> "Unknown entity type"
        }
    }
    
    private fun parseConnectionResponse(response: String, entityId1: String, entityId2: String): List<Connection> {
        return try {
            // Extract JSON from response
            val jsonMatch = Regex("```json\\s*(\\{.*?\\})\\s*```", RegexOption.DOT_MATCHES_ALL)
                .find(response)
                ?.groupValues?.get(1)
                ?: response.trim()
            
            // Parse connections (simplified - would need proper JSON parsing)
            emptyList<Connection>() // Placeholder
        } catch (e: Exception) {
            logger.error(e) { "Failed to parse connection response" }
            emptyList()
        }
    }
    
    private fun getEntity(entityId: String, entityType: String): Any {
        return when (entityType.lowercase()) {
            "concept" -> conceptRepository.findById(entityId).orElseThrow()
            "document" -> sourceDocumentRepository.findById(entityId).orElseThrow()
            else -> throw IllegalArgumentException("Unknown entity type: $entityType")
        }
    }
    
    private fun searchSimilarEntities(
        embedding: FloatArray,
        threshold: Double,
        maxResults: Int
    ): List<SimilarEntity> {
        // Simplified Weaviate search - would need proper implementation
        return emptyList()
    }
    
    private fun generateSimilarityExplanation(concept: Concept, similarEntity: SimilarEntity): String {
        return "Concept '${concept.name}' shares semantic similarity with ${similarEntity.type} '${similarEntity.name}' based on vector embedding analysis."
    }
    
    /**
     * Suggest new connections for a concept
     */
    suspend fun suggestConnections(conceptId: String): List<Connection> {
        logger.info { "Suggesting connections for concept: $conceptId" }
        
        val concept = conceptRepository.findById(conceptId).orElseThrow()
        val suggestions = mutableListOf<Connection>()
        
        // Find semantic similarities
        val similarities = findSemanticSimilarities(conceptId)
        similarities.forEach { similarity ->
            if (similarity.similarity >= 0.8) {
                suggestions.add(
                    Connection(
                        fromEntity = conceptId,
                        toEntity = similarity.entityId,
                        relationshipType = "SEMANTICALLY_SIMILAR",
                        confidence = similarity.similarity,
                        reasoning = similarity.explanation,
                        evidence = listOf("Vector similarity: ${similarity.similarity}")
                    )
                )
            }
        }
        
        // Use LLM to find additional connections
        val allConcepts = conceptRepository.findAll()
        allConcepts.filter { it.id != conceptId }.take(10).forEach { otherConcept ->
            val connections = reasonAboutConnections(
                conceptId, "concept",
                otherConcept.id, "concept"
            )
            suggestions.addAll(connections)
        }
        
        return suggestions.sortedByDescending { it.confidence }
    }
    
    private data class SimilarEntity(
        val id: String,
        val name: String,
        val type: String,
        val similarity: Double
    )
}
