package com.tactorder.rdss.domain

import org.springframework.data.annotation.Id
import org.springframework.data.neo4j.core.schema.Node
import org.springframework.data.neo4j.core.schema.Property
import org.springframework.data.neo4j.core.schema.Relationship
import java.time.LocalDateTime

@Node
data class Experiment(
    @Id
    val id: String,
    
    @Property
    val name: String,
    
    @Property
    val date: LocalDateTime,
    
    @Property
    val hypothesis: String,
    
    @Property
    val methodology: String?,
    
    @Property
    val resultsSummary: String?,
    
    @Property
    val conclusion: String?,
    
    @Property
    val codeRepo: String?,
    
    @Property
    val status: ExperimentStatus,
    
    @Relationship(type = "VALIDATES", direction = Relationship.Direction.OUTGOING)
    val validates: List<Concept> = emptyList(),
    
    @Relationship(type = "CHALLENGES", direction = Relationship.Direction.OUTGOING)
    val challenges: List<Concept> = emptyList()
)

enum class ExperimentStatus {
    PLANNED, IN_PROGRESS, COMPLETED, FAILED
}
