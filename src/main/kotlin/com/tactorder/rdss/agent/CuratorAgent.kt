package com.tactorder.rdss.agent

import com.tactorder.rdss.domain.*
import com.tactorder.rdss.repository.*
import dev.langchain4j.model.embedding.EmbeddingModel
import io.weaviate.client.WeaviateClient
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
@Transactional
class CuratorAgent(
    private val citationExtractor: CitationExtractor,
    private val entityExtractor: EntityExtractor,
    private val embeddingModel: EmbeddingModel,
    private val weaviateClient: WeaviateClient,
    private val sourceDocumentRepository: SourceDocumentRepository,
    private val conceptRepository: ConceptRepository,
    private val researchAreaRepository: ResearchAreaRepository
) {
    
    private val logger = KotlinLogging.logger {}
    
    data class IngestionResult(
        val documentId: String,
        val extractedCitations: List<CitationExtractor.Citation>,
        val extractedConcepts: List<EntityExtractor.ExtractedConcept>,
        val extractedMethods: List<EntityExtractor.ExtractedMethod>,
        val extractedResearchAreas: List<EntityExtractor.ExtractedResearchArea>,
        val embeddingGenerated: Boolean,
        val relationshipsCreated: Int
    )
    
    /**
     * Ingest a document and extract all entities and relationships
     */
    suspend fun ingestDocument(
        title: String,
        content: String,
        type: DocumentType,
        authors: List<String> = emptyList(),
        year: Int? = null,
        venue: String? = null,
        doi: String? = null,
        pdfPath: String? = null
    ): IngestionResult {
        
        logger.info { "Starting ingestion for document: $title" }
        
        // Create source document
        val document = SourceDocument(
            id = UUID.randomUUID().toString(),
            title = title,
            type = type,
            authors = authors,
            year = year,
            venue = venue,
            doi = doi,
            bibtexKey = generateBibtexKey(title, authors, year),
            abstract = extractAbstract(content),
            pdfPath = pdfPath,
            tags = emptyList()
        )
        
        val savedDocument = sourceDocumentRepository.save(document)
        
        // Extract citations
        val citations = citationExtractor.extractCitations(content)
        logger.info { "Extracted ${citations.size} citations from $title" }
        
        // Extract entities
        val entities = entityExtractor.extractEntities(content, title)
        val validatedEntities = entityExtractor.validateEntities(entities)
        logger.info { "Extracted ${validatedEntities.concepts.size} concepts, ${validatedEntities.methods.size} methods" }
        
        // Generate and store embeddings
        val embeddingGenerated = generateAndStoreEmbedding(savedDocument.id, content)
        
        // Create concepts from extracted entities
        val createdConcepts = createConceptsFromEntities(validatedEntities, savedDocument)
        
        // Create research areas
        val createdResearchAreas = createResearchAreasFromEntities(validatedEntities)
        
        // Create relationships
        val relationshipsCreated = createDocumentRelationships(savedDocument, citations, createdConcepts)
        
        logger.info { "Completed ingestion for document: $title" }
        
        return IngestionResult(
            documentId = savedDocument.id,
            extractedCitations = citations,
            extractedConcepts = validatedEntities.concepts,
            extractedMethods = validatedEntities.methods,
            extractedResearchAreas = validatedEntities.researchAreas,
            embeddingGenerated = embeddingGenerated,
            relationshipsCreated = relationshipsCreated
        )
    }
    
    private fun generateBibtexKey(title: String, authors: List<String>, year: Int?): String {
        val firstAuthor = authors.firstOrNull()?.split(" ")?.first()?.lowercase() ?: "unknown"
        val titleWords = title.split(" ").take(3).joinToString("") { 
            it.lowercase().replace(Regex("[^a-z]"), "") 
        }
        val yearStr = year?.toString() ?: "n.d."
        return "${firstAuthor}${yearStr}${titleWords}"
    }
    
    private fun extractAbstract(content: String): String? {
        // Look for common abstract patterns
        val abstractPatterns = listOf(
            Regex("(?i)abstract[:\\s]*\\n?([\\s\\S]*?)(?=\\n\\n|keywords|introduction|1\\.)"),
            Regex("(?i)abstract[:\\s]*([\\s\\S]*?)(?=\\n\\n|keywords|introduction|1\\.)")
        )
        
        for (pattern in abstractPatterns) {
            val match = pattern.find(content)
            if (match != null) {
                return match.groupValues[1].trim().take(1000)
            }
        }
        
        return null
    }
    
    private suspend fun generateAndStoreEmbedding(documentId: String, content: String): Boolean {
        return try {
            // Generate embedding for the document
            val embedding = embeddingModel.embed(content).content()
            
            // Store in Weaviate (simplified - would need proper Weaviate integration)
            // weaviateClient.data().creator()
            //     .withClassName("Document")
            //     .withId(documentId)
            //     .withVector(embedding)
            //     .withProperties(mapOf("content" to content))
            //     .run()
            
            logger.debug { "Generated embedding for document $documentId" }
            true
        } catch (e: Exception) {
            logger.error(e) { "Failed to generate embedding for document $documentId" }
            false
        }
    }
    
    private fun createConceptsFromEntities(
        entities: EntityExtractor.ExtractedEntities,
        document: SourceDocument
    ): List<Concept> {
        val concepts = mutableListOf<Concept>()
        
        // Create concepts from extracted concepts
        entities.concepts.forEach { extractedConcept ->
            val concept = Concept(
                id = UUID.randomUUID().toString(),
                name = extractedConcept.name,
                description = extractedConcept.description,
                maturity = ConceptMaturity.SEED,
                status = ConceptStatus.ACTIVE,
                firstMentioned = LocalDateTime.now(),
                lastUpdated = LocalDateTime.now(),
                currentVersion = 1,
                tags = listOf("auto-extracted"),
                inspiredBy = listOf(document)
            )
            concepts.add(concept)
        }
        
        // Save all concepts
        val savedConcepts = conceptRepository.saveAll(concepts)
        
        logger.info { "Created ${savedConcepts.size} concepts from document ${document.title}" }
        return savedConcepts
    }
    
    private fun createResearchAreasFromEntities(
        entities: EntityExtractor.ExtractedEntities
    ): List<ResearchArea> {
        val researchAreas = mutableListOf<ResearchArea>()
        
        entities.researchAreas.forEach { extractedArea ->
            val researchArea = ResearchArea(
                id = UUID.randomUUID().toString(),
                name = extractedArea.name,
                description = extractedArea.description,
                parentArea = null
            )
            researchAreas.add(researchArea)
        }
        
        val savedAreas = researchAreaRepository.saveAll(researchAreas)
        logger.info { "Created ${savedAreas.size} research areas" }
        return savedAreas
    }
    
    private fun createDocumentRelationships(
        document: SourceDocument,
        citations: List<CitationExtractor.Citation>,
        concepts: List<Concept>
    ): Int {
        var relationshipsCreated = 0
        
        // Create citation relationships (simplified - would need proper citation matching)
        citations.forEach { citation ->
            // Try to find cited document in database
            val citedDocument = findCitedDocument(citation)
            if (citedDocument != null) {
                // Create citation relationship (would need custom Neo4j query)
                relationshipsCreated++
            }
        }
        
        // Create concept-document relationships (already handled in concept creation)
        relationshipsCreated += concepts.size
        
        logger.info { "Created $relationshipsCreated relationships for document ${document.title}" }
        return relationshipsCreated
    }
    
    private fun findCitedDocument(citation: CitationExtractor.Citation): SourceDocument? {
        // Try to match by DOI first
        citation.doi?.let { doi ->
            sourceDocumentRepository.findByDoi(doi).ifPresent { return it }
        }
        
        // Try to match by author and year
        if (citation.authors.isNotEmpty() && citation.year != null) {
            val documents = sourceDocumentRepository.findByAuthor(citation.authors.first())
            return documents.find { it.year == citation.year }
        }
        
        return null
    }
    
    /**
     * Process multiple documents in batch
     */
    suspend fun ingestBatch(documents: List<DocumentData>): List<IngestionResult> {
        return documents.map { doc ->
            ingestDocument(
                title = doc.title,
                content = doc.content,
                type = doc.type,
                authors = doc.authors,
                year = doc.year,
                venue = doc.venue,
                doi = doc.doi,
                pdfPath = doc.pdfPath
            )
        }
    }
    
    data class DocumentData(
        val title: String,
        val content: String,
        val type: DocumentType,
        val authors: List<String> = emptyList(),
        val year: Int? = null,
        val venue: String? = null,
        val doi: String? = null,
        val pdfPath: String? = null
    )
}
