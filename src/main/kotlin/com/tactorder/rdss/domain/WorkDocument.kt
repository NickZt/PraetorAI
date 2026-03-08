package com.tactorder.rdss.domain

import org.springframework.data.annotation.Id
import org.springframework.data.neo4j.core.schema.Node
import org.springframework.data.neo4j.core.schema.Property
import org.springframework.data.neo4j.core.schema.Relationship
import java.time.LocalDate

@Node
data class WorkDocument(
    @Id
    val id: String,

    @Property
    val title: String,

    @Property
    val type: WorkDocumentType,

    @Property
    val status: WorkDocumentStatus,

    @Property
    val filePath: String,

    @Property
    val deadline: LocalDate?,

    @Relationship(type = "APPLIES", direction = Relationship.Direction.OUTGOING)
    val appliedConcepts: List<Concept> = emptyList(),

    @Relationship(type = "CITES", direction = Relationship.Direction.OUTGOING)
    val citations: List<SourceDocument> = emptyList()
)

enum class WorkDocumentType {
    RESEARCH_PAPER, GRANT_PROPOSAL, PRESENTATION, TECHNICAL_REPORT, BOOK_CHAPTER
}

enum class WorkDocumentStatus {
    DRAFT, REVIEW, SUBMITTED, PUBLISHED, REJECTED
}
