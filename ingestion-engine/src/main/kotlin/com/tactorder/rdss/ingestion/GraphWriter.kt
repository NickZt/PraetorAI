package com.tactorder.rdss.ingestion

import org.neo4j.ogm.config.Configuration
import org.neo4j.ogm.session.Session
import org.neo4j.ogm.session.SessionFactory

class GraphWriter(private val config: io.vertx.core.json.JsonObject) {

    private val sessionFactory: SessionFactory

    init {
        val neo4jConfig = Configuration.Builder()
            .uri(config.getString("neo4j.uri", "bolt://localhost:7687"))
            .credentials(
                config.getString("neo4j.username", "neo4j"),
                config.getString("neo4j.password", "password")
            )
            .build()

        sessionFactory = SessionFactory(neo4jConfig, "com.tactorder.rdss.domain")
    }

    fun getSession(): Session {
        return sessionFactory.openSession()
    }

    fun isDocumentIngested(md5Hash: String): Boolean {
        val session = getSession()
        val result = session.query("MATCH (d:Document {md5Hash: \$hash}) RETURN count(d) as count", mapOf("hash" to md5Hash))
        val count = result.firstOrNull()?.get("count") as? Long ?: 0L
        return count > 0
    }

    fun saveEntities(entities: List<Any>) {
        val session = getSession()
        session.beginTransaction().use { tx ->
            entities.forEach { entity ->
                // DEPRECATED: Standard OGM save() issues destructive MERGE operations on properties.
                // Refactor to use appendActionNode() for temporal history.
                session.save(entity)
            }
            tx.commit()
        }
    }

    /**
     * SOP 3.1: Temporal Versioning via append-only ActionNodes.
     * Replaces destructive in-place property updates.
     */
    fun appendActionNode(targetNodeId: String, actionType: String, properties: Map<String, Any>, startDate: String, endDate: String? = null) {
        val session = getSession()
        val cypher = """
            MATCH (target) WHERE target.id = ${'$'}targetId
            CREATE (action:ActionNode {
                type: ${'$'}actionType,
                StartDate: datetime(${'$'}startDate),
                EndDate: case when ${'$'}endDate is not null then datetime(${'$'}endDate) else null end,
                timestamp: datetime()
            })
            SET action += ${'$'}props
            CREATE (target)-[:HAS_ACTION]->(action)
        """.trimIndent()
        
        val params = mapOf(
            "targetId" to targetNodeId,
            "actionType" to actionType,
            "startDate" to startDate,
            "endDate" to endDate,
            "props" to properties
        )
        
        session.query(cypher, params)
    }

    // Specific methods for extracted JSON to Domain Entity mapping would go here
    // or in a separate mapper service.
}
