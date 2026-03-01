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
                relationshipFilter: 'MENTIONS|RELATED_TO|AMENDS|CONTRADICTS',
                labelFilter: '+Concept|+Law|+Section'
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
    
    // Placeholder for temporal filtering logic
    fun temporalFilter(graphData: List<JsonObject>, queryDate: LocalDate?): List<JsonObject> {
        if (queryDate == null) return graphData
        
        // Filter logic: Check 'validFrom' / 'validTo' properties on Law nodes
        // For relationships like AMENDS, check 'date'
        
        return graphData.filter { item ->
            val props = item.getJsonObject("props") ?: return@filter true
            
            // Check validFrom/validTo
            val validFrom = props.getString("validFrom")?.let { LocalDate.parse(it) }
            val validTo = props.getString("validTo")?.let { LocalDate.parse(it) }
            
            if (validFrom != null && queryDate.isBefore(validFrom)) return@filter false
            if (validTo != null && queryDate.isAfter(validTo)) return@filter false
            
            true
        }
    }
}
