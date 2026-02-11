package com.tactorder.rdss.domain

import org.springframework.data.annotation.Id
import org.springframework.data.neo4j.core.schema.Node
import org.springframework.data.neo4j.core.schema.Property
import org.springframework.data.neo4j.core.schema.Relationship

@Node
data class ResearchArea(
    @Id
    val id: String,
    
    @Property
    val name: String,
    
    @Property
    val description: String?,
    
    @Relationship(type = "SUBAREA_OF", direction = Relationship.Direction.OUTGOING)
    val parentArea: ResearchArea?
)
