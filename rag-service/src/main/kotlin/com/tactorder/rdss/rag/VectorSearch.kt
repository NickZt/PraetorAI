package com.tactorder.rdss.rag

import dev.langchain4j.model.embedding.EmbeddingModel
import io.vertx.core.json.JsonObject
import org.neo4j.driver.Driver
import org.slf4j.LoggerFactory

class VectorSearch(
    private val driver: Driver,
    private val embeddingModel: EmbeddingModel
) {
    private val logger = LoggerFactory.getLogger(VectorSearch::class.java)
    private val indexName = "document_embeddings"
    private val dimension = 384 // AllMiniLmL6V2 dimension

    init {
        createIndexIfNotExists()
    }

    private fun createIndexIfNotExists() {
        // Create Vector Index for Document nodes on property 'embedding'
        // Using Neo4j 5.x syntax
        val query = """
            CREATE VECTOR INDEX $indexName IF NOT EXISTS
            FOR (d:Document) ON (d.embedding)
            OPTIONS {indexConfig: {
             `vector.dimensions`: $dimension,
             `vector.similarity_function`: 'cosine'
            }}
        """

        try {
            driver.session().use { session ->
                session.run(query)
                logger.info("Vector index '$indexName' ensured.")
            }
        } catch (e: Exception) {
            logger.error("Failed to create vector index", e)
        }
    }

    fun search(query: String, limit: Int = 5): List<JsonObject> {
        val embedding = embeddingModel.embed(query).content()
        val vectorList = embedding.vector().toList()

        val cypher = """
            CALL db.index.vector.queryNodes($indexName, $limit, $vectorList)
            YIELD node, score
            RETURN node.id AS id, node.title AS title, node.rawText AS text, score
        """

        return driver.session().use { session ->
            session.run(cypher).list().map { record ->
                JsonObject()
                    .put("id", record.get("id").asLong())
                    .put("title", record.get("title").asString())
                    .put("text", record.get("text").asString())
                    .put("score", record.get("score").asDouble())
            }
        }
    }
}
