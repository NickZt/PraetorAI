package com.tactorder.rdss.rag

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import org.neo4j.driver.Driver
import java.time.LocalDate

class GraphRetriever(private val driver: Driver) {

    fun retrieveContext(nodeIds: List<Long>, depth: Int = 2): List<JsonObject> {
        if (nodeIds.isEmpty()) return emptyList()

        // Cypher to traverse 2 hops from start nodes
        // Collects relationships and neighbor nodes
        val cypher = """
            MATCH (start) WHERE id(start) IN ${'$'}ids
            CALL apoc.path.subgraphAll(start, {
                maxLevel: $depth,
                relationshipFilter: 'MENTIONS|RELATED_TO|AMENDS|CONTRADICTS|>HAS_ACTION',
                labelFilter: '+Concept|+Law|+Section|+ActionNode'
            })
            YIELD nodes, relationships
            RETURN nodes, relationships
        """

        return driver.session().use { session ->
            val result = session.run(cypher, mapOf("ids" to nodeIds))
            if (result.hasNext()) {
                val record = result.next()
                val nodes = record.get("nodes").asList { node ->
                    val n = node as org.neo4j.driver.types.Node
                    JsonObject()
                        .put("id", n.elementId())
                        .put("labels", JsonArray(n.labels().toList()))
                        .put("props", JsonObject(n.asMap()))
                }
                val rels = record.get("relationships").asList { rel ->
                    val r = rel as org.neo4j.driver.types.Relationship
                    JsonObject()
                        .put("type", r.type())
                        .put("start", r.startNodeElementId())
                        .put("end", r.endNodeElementId())
                        .put("props", JsonObject(r.asMap()))
                }

                // Combine into a generic result structure
                // or just return the subgraph list
                nodes + rels
            } else {
                emptyList()
            }
        }
    }

    // SOP 3.1: Temporal filtering logic using ActionNode metadata
    fun temporalFilter(graphData: List<JsonObject>, queryDate: LocalDate?): List<JsonObject> {
        if (queryDate == null) return graphData

        return graphData.filter { item ->
            val props = item.getJsonObject("props") ?: return@filter true

            // SOP 3.1 checks StartDate/EndDate
            val validFromStr = props.getString("StartDate") ?: props.getString("validFrom")
            val validToStr = props.getString("EndDate") ?: props.getString("validTo")

            val validFrom = validFromStr?.substringBefore("T")?.let { LocalDate.parse(it) }
            val validTo = validToStr?.substringBefore("T")?.let { LocalDate.parse(it) }

            if (validFrom != null && queryDate.isBefore(validFrom)) return@filter false
            if (validTo != null && queryDate.isAfter(validTo)) return@filter false

            true
        }
    }
}
