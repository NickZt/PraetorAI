package com.tactorder.rdss.domain

import org.springframework.data.annotation.Id
import org.springframework.data.neo4j.core.schema.Node
import org.springframework.data.neo4j.core.schema.Property
import org.springframework.data.neo4j.core.schema.Relationship
import java.time.LocalDateTime

@Node
data class Concept(
    @Id
    val id: String,
    
    @Property
    val name: String,
    
    @Property
    val description: String,
    
    @Property
    val maturity: ConceptMaturity,
    
    @Property
    val status: ConceptStatus,
    
    @Property
    val firstMentioned: LocalDateTime,
    
    @Property
    val lastUpdated: LocalDateTime,
    
    @Property
    val currentVersion: Int,
    
    @Property
    val tags: List<String>,
    
    @Relationship(type = "BUILDS_ON", direction = Relationship.Direction.OUTGOING)
    val buildsOn: List<Concept> = emptyList(),
    
    @Relationship(type = "INSPIRED_BY", direction = Relationship.Direction.OUTGOING)
    val inspiredBy: List<SourceDocument> = emptyList(),
    
    @Relationship(type = "CONTRADICTS", direction = Relationship.Direction.OUTGOING)
    val contradicts: List<Concept> = emptyList(),
    
    @Relationship(type = "BELONGS_TO", direction = Relationship.Direction.OUTGOING)
    val belongsTo: List<ResearchArea> = emptyList(),
    
    @Relationship(type = "HAS_VERSION", direction = Relationship.Direction.OUTGOING)
    val versions: List<ConceptVersion> = emptyList()
)

enum class ConceptMaturity {
    SEED, DEVELOPING, MATURE, VALIDATED
}

enum class ConceptStatus {
    ACTIVE, ON_HOLD, ARCHIVED, MERGED
}
