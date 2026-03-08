package com.tactorder.rdss.repository

import com.tactorder.rdss.domain.Concept
import com.tactorder.rdss.domain.ConceptMaturity
import com.tactorder.rdss.domain.ConceptStatus
import org.springframework.data.neo4j.repository.Neo4jRepository
import org.springframework.data.neo4j.repository.query.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

@Repository
interface ConceptRepository : Neo4jRepository<Concept, String> {

    @Query("MATCH (c:Concept) WHERE c.maturity = \$maturity RETURN c")
    fun findByMaturity(maturity: ConceptMaturity): List<Concept>

    @Query("MATCH (c:Concept) WHERE c.status = \$status RETURN c")
    fun findByStatus(status: ConceptStatus): List<Concept>

    @Query("MATCH (c:Concept) WHERE c.lastUpdated >= \$since RETURN c ORDER BY c.lastUpdated DESC")
    fun findRecentlyUpdated(since: LocalDateTime): List<Concept>

    @Query(
        """
        MATCH (c:Concept)
        WHERE NOT EXISTS((:Experiment)-[:VALIDATES]->(c))
        AND c.lastUpdated >= \$since
        RETURN c
        ORDER BY c.lastUpdated DESC
    """
    )
    fun findConceptsWithoutValidationSince(since: LocalDateTime): List<Concept>

    @Query(
        """
        MATCH (c1:Concept)-[:BUILDS_ON*1..3]->(c2:Concept)
        WHERE c1.id = \$conceptId
        RETURN DISTINCT c2
    """
    )
    fun findRelatedConcepts(conceptId: String): List<Concept>

    @Query(
        """
        MATCH path = shortestPath(
            (c1:Concept)-[*1..4]-(c2:Concept)
        )
        WHERE c1.id = \$conceptId1 AND c2.id = \$conceptId2
        RETURN path
    """
    )
    fun findShortestPath(conceptId1: String, conceptId2: String): Optional<List<Concept>>

    @Query(
        """
        MATCH (c:Concept)
        WHERE \$tag IN c.tags
        RETURN c
    """
    )
    fun findByTag(tag: String): List<Concept>

    @Query(
        """
        MATCH (c:Concept)-[:BELONGS_TO]->(ra:ResearchArea)
        WHERE ra.name = \$areaName
        RETURN c
    """
    )
    fun findByResearchArea(areaName: String): List<Concept>

    @Query(
        """
        MATCH (c:Concept)-[:INSPIRED_BY]->(sd:SourceDocument)
        WHERE sd.year >= \$year
        RETURN DISTINCT c
    """
    )
    fun findConceptsInspiredByDocumentsSince(year: Int): List<Concept>

    @Query(
        """
        MATCH (c:Concept)-[:CONTRADICTS]->(other:Concept)
        WHERE c.id = \$conceptId
        RETURN other
    """
    )
    fun findContradictoryConcepts(conceptId: String): List<Concept>
}
