package com.tactorder.rdss.domain

import org.springframework.data.annotation.Id
import org.springframework.data.neo4j.core.schema.Node
import org.springframework.data.neo4j.core.schema.Property
import org.springframework.data.neo4j.core.schema.Relationship

@Node
data class SourceDocument(
    @Id
    val id: String,

    @Property
    val title: String,

    @Property
    val type: DocumentType,

    @Property
    val authors: List<String>,

    @Property
    val year: Int?,

    @Property
    val venue: String?,

    @Property
    val doi: String?,

    @Property
    val bibtexKey: String?,

    @Property
    val abstract: String?,

    @Property
    val pdfPath: String?,

    @Property
    val tags: List<String>,

    @Relationship(type = "CITES", direction = Relationship.Direction.OUTGOING)
    val citations: List<SourceDocument> = emptyList(),

    @Relationship(type = "BELONGS_TO", direction = Relationship.Direction.OUTGOING)
    val belongsTo: List<ResearchArea> = emptyList()
)

enum class DocumentType {
    ACADEMIC_PAPER, BOOK, TECHNICAL_REPORT, THESIS, PATENT, BLOG_POST, PRESENTATION
}
