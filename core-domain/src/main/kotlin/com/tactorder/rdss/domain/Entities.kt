package com.tactorder.rdss.domain

import org.neo4j.ogm.annotation.*
import java.time.LocalDate

@NodeEntity
data class Document(
    @Id @GeneratedValue var id: Long? = null,
    var title: String,
    var rawText: String? = null,
    var uri: String? = null,
    var md5Hash: String? = null,
    var fileSize: Long? = null
) {
    @Relationship(type = "MENTIONS")
    var concepts: MutableSet<Concept> = mutableSetOf()

    @Relationship(type = "HAS_CHUNK")
    var chunks: MutableList<Chunk> = mutableListOf()
}

@NodeEntity
data class Law(
    @Id @GeneratedValue var id: Long? = null,
    var type: String, // e.g., "Constitution", "Decree"
    var number: String,
    var title: String,
    var validFrom: LocalDate? = null,
    var validTo: LocalDate? = null
) {
    @Relationship(type = "AMENDS")
    var amends: MutableSet<Amends> = mutableSetOf()

    @Relationship(type = "MENTIONS")
    var concepts: MutableSet<Concept> = mutableSetOf()

    @Relationship(type = "CONTAINS")
    var sections: MutableSet<Section> = mutableSetOf()
}

@RelationshipEntity(type = "AMENDS")
data class Amends(
    @Id @GeneratedValue var id: Long? = null,
    @StartNode var source: Law,
    @EndNode var target: Law,
    var date: LocalDate? = null,
    var summary: String? = null
)

@NodeEntity
data class Section(
    @Id @GeneratedValue var id: Long? = null,
    var content: String,
    var sectionNumber: String
) {
    @Relationship(type = "CONTRADICTS")
    var contradicts: MutableSet<Section> = mutableSetOf()
}

@NodeEntity
data class Concept(
    @Id @GeneratedValue var id: Long? = null,
    var name: String,
    var description: String? = null
) {
    @Relationship(type = "RELATED_TO")
    var relatedConcepts: MutableSet<Concept> = mutableSetOf()
}

@NodeEntity
data class Person(
    @Id @GeneratedValue var id: Long? = null,
    var name: String,
    var role: String? = null
)

@NodeEntity
data class Chunk(
    @Id @GeneratedValue var id: Long? = null,
    var text: String,
    var embedding: DoubleArray? = null,
    var index: Int = 0
)

@NodeEntity
data class DateNode(
    @Id @GeneratedValue var id: Long? = null,
    var date: LocalDate
)
