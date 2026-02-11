package com.tactorder.rdss.repository

import com.tactorder.rdss.domain.DocumentType
import com.tactorder.rdss.domain.SourceDocument
import org.springframework.data.neo4j.repository.Neo4jRepository
import org.springframework.data.neo4j.repository.query.Query
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface SourceDocumentRepository : Neo4jRepository<SourceDocument, String> {
    
    @Query("MATCH (sd:SourceDocument) WHERE sd.type = \$type RETURN sd")
    fun findByType(type: DocumentType): List<SourceDocument>
    
    @Query("MATCH (sd:SourceDocument) WHERE sd.year = \$year RETURN sd ORDER BY sd.title")
    fun findByYear(year: Int): List<SourceDocument>
    
    @Query("MATCH (sd:SourceDocument) WHERE \$author IN sd.authors RETURN sd")
    fun findByAuthor(author: String): List<SourceDocument>
    
    @Query("MATCH (sd:SourceDocument) WHERE sd.venue = \$venue RETURN sd")
    fun findByVenue(venue: String): List<SourceDocument>
    
    @Query("""
        MATCH (sd1:SourceDocument)-[:CITES]->(sd2:SourceDocument)
        WHERE sd1.id = \$documentId
        RETURN sd2
    """)
    fun findCitations(documentId: String): List<SourceDocument>
    
    @Query("""
        MATCH (sd1:SourceDocument)<-[:CITES]-(sd2:SourceDocument)
        WHERE sd1.id = \$documentId
        RETURN sd2
    """)
    fun findCitedBy(documentId: String): List<SourceDocument>
    
    @Query("""
        MATCH (sd1:SourceDocument)-[:CITES]->(common:SourceDocument)<-[:CITES]-(sd2:SourceDocument)
        WHERE sd1.id = \$documentId1 AND sd2.id = \$documentId2
        RETURN COUNT(common) as commonCitations
    """)
    fun findBibliographicCoupling(documentId1: String, documentId2: String): Int
    
    @Query("""
        MATCH (sd:SourceDocument)
        WHERE toLower(sd.title) CONTAINS toLower(\$keyword)
           OR toLower(sd.abstract) CONTAINS toLower(\$keyword)
        RETURN sd
    """)
    fun searchByKeyword(keyword: String): List<SourceDocument>
    
    @Query("""
        MATCH (sd:SourceDocument)-[:BELONGS_TO]->(ra:ResearchArea)
        WHERE ra.name = \$areaName
        RETURN sd
    """)
    fun findByResearchArea(areaName: String): List<SourceDocument>
    
    @Query("MATCH (sd:SourceDocument) WHERE sd.doi = \$doi RETURN sd")
    fun findByDoi(doi: String): Optional<SourceDocument>
    
    @Query("MATCH (sd:SourceDocument) WHERE sd.bibtexKey = \$bibtexKey RETURN sd")
    fun findByBibtexKey(bibtexKey: String): Optional<SourceDocument>
}
