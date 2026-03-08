package com.tactorder.rdss.repository

import com.tactorder.rdss.domain.Experiment
import com.tactorder.rdss.domain.ExperimentStatus
import org.springframework.data.neo4j.repository.Neo4jRepository
import org.springframework.data.neo4j.repository.query.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface ExperimentRepository : Neo4jRepository<Experiment, String> {

    @Query("MATCH (e:Experiment) WHERE e.status = \$status RETURN e ORDER BY e.date DESC")
    fun findByStatus(status: ExperimentStatus): List<Experiment>

    @Query("MATCH (e:Experiment) WHERE e.date >= \$since RETURN e ORDER BY e.date DESC")
    fun findSince(since: LocalDateTime): List<Experiment>

    @Query(
        """
        MATCH (e:Experiment)-[:VALIDATES]->(c:Concept)
        WHERE c.id = \$conceptId
        RETURN e
        ORDER BY e.date DESC
    """
    )
    fun findByValidatedConcept(conceptId: String): List<Experiment>

    @Query(
        """
        MATCH (e:Experiment)-[:CHALLENGES]->(c:Concept)
        WHERE c.id = \$conceptId
        RETURN e
        ORDER BY e.date DESC
    """
    )
    fun findByChallengedConcept(conceptId: String): List<Experiment>

    @Query(
        """
        MATCH (e:Experiment)-[:VALIDATES]->(c:Concept)
        WHERE c.maturity = \$maturity
        RETURN DISTINCT e
        ORDER BY e.date DESC
    """
    )
    fun findByConceptMaturity(maturity: String): List<Experiment>

    @Query(
        """
        MATCH (e:Experiment)
        WHERE toLower(e.name) CONTAINS toLower(\$keyword)
           OR toLower(e.hypothesis) CONTAINS toLower(\$keyword)
        RETURN e
    """
    )
    fun searchByKeyword(keyword: String): List<Experiment>
}
