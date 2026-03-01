package com.tactorder.rdss.ingestion

import com.tactorder.rdss.domain.Document
import com.tactorder.rdss.domain.Law
import com.tactorder.rdss.domain.Concept
import org.neo4j.ogm.session.Session
import org.neo4j.ogm.session.SessionFactory
import org.neo4j.ogm.config.Configuration

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
    
    fun saveEntities(entities: List<Any>) {
        val session = getSession()
        session.beginTransaction().use { tx ->
            // Batch save if possible, or iterate
            entities.forEach { entity ->
                session.save(entity)
            }
            tx.commit()
        }
    }
    
    // Specific methods for extracted JSON to Domain Entity mapping would go here
    // or in a separate mapper service.
}
