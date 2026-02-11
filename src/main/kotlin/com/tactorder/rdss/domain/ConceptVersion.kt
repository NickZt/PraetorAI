package com.tactorder.rdss.domain

import org.springframework.data.annotation.Id
import org.springframework.data.neo4j.core.schema.Node
import org.springframework.data.neo4j.core.schema.Property
import org.springframework.data.neo4j.core.schema.Relationship
import java.time.LocalDateTime

@Node
data class ConceptVersion(
    @Id
    val id: String,
    
    @Property
    val conceptId: String,
    
    @Property
    val version: Int,
    
    @Property
    val date: LocalDateTime,
    
    @Property
    val description: String,
    
    @Property
    val keyInsights: List<String>,
    
    @Property
    val changes: List<String>,
    
    @Property
    val reason: String?,
    
    @Relationship(type = "EVOLVED_FROM", direction = Relationship.Direction.OUTGOING)
    val evolvedFrom: ConceptVersion?,
    
    @Relationship(type = "RELATED_WORK", direction = Relationship.Direction.OUTGOING)
    val relatedWork: List<SourceDocument> = emptyList()
)
