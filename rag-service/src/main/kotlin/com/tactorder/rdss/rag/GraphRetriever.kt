package com.tactorder.rdss.rag

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import org.neo4j.driver.Driver
import java.time.LocalDate

class GraphRetriever(private val driver: Driver) {

    fun retrieveContext(nodeIds: List<Long>, depth: Int = 2, queryDate: LocalDate? = null): List<JsonObject> {
        if (nodeIds.isEmpty()) return emptyList()

        val dateParam = queryDate?.toString()?.let { "${it}T00:00:00Z" }

        // MNNLLama / Praetor AI SOP 3.1: Strict Native Temporal Graph queries
        // If a date is provided, we filter out ActionNodes that violate the StartDate and EndDate bounds.
        val cypher = if (dateParam != null) {
            """
            MATCH (start) WHERE id(start) IN ${'$'}ids
            CALL apoc.path.subgraphAll(start, {
                maxLevel: ${'$'}depth,
                relationshipFilter: 'MENTIONS|RELATED_TO|AMENDS|CONTRADICTS|>HAS_ACTION|<HAS_CHUNK',
                labelFilter: '+Concept|+Law|+Section|+ActionNode|+Document|+Chunk'
            })
            YIELD nodes, relationships
            WITH [n IN nodes WHERE NOT 'ActionNode' IN labels(n) OR 
                 (n.StartDate <= datetime(${'$'}queryDate) AND (n.EndDate IS NULL OR n.EndDate > datetime(${'$'}queryDate)))] AS validNodes, relationships
            RETURN validNodes as nodes, relationships
            """
        } else {
             """
            MATCH (start) WHERE id(start) IN ${'$'}ids
            CALL apoc.path.subgraphAll(start, {
                maxLevel: ${'$'}depth,
                relationshipFilter: 'MENTIONS|RELATED_TO|AMENDS|CONTRADICTS|>HAS_ACTION',
                labelFilter: '+Concept|+Law|+Section|+ActionNode'
            })
            YIELD nodes, relationships
            RETURN nodes, relationships
            """
        }

        return driver.session().use { session ->
            val result = session.run(cypher, mapOf("ids" to nodeIds, "depth" to depth, "queryDate" to dateParam))
            if (result.hasNext()) {
                val record = result.next()
                val nodes = record.get("nodes").asList { node ->
                    val n = node.asNode()
                    JsonObject()
                        .put("id", n.id())
                        .put("labels", JsonArray(n.labels().toList()))
                        .put("props", JsonObject(n.asMap()))
                }
                val rels = record.get("relationships").asList { rel ->
                    val r = rel.asRelationship()
                    JsonObject()
                        .put("type", r.type())
                        .put("start", r.startNodeId())
                        .put("end", r.endNodeId())
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

    // Note: The Kotlin side temporalFilter() was completely removed 
    // to strictly enforce the Praetor AI architectural rule that anachronisms
    // must be eliminated at the Database layer, avoiding excessive JVM memory load.
}
