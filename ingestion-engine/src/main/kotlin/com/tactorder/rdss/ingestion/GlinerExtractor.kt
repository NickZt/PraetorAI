package com.tactorder.rdss.ingestion

import com.tactorder.rdss.domain.Concept
import com.tactorder.rdss.domain.Document
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.WebClientOptions
import io.vertx.kotlin.coroutines.await
import org.slf4j.LoggerFactory

class GlinerExtractor(private val vertx: io.vertx.core.Vertx, private val config: JsonObject) {
    private val logger = LoggerFactory.getLogger(GlinerExtractor::class.java)
    private val webClient = WebClient.create(vertx, WebClientOptions().setConnectTimeout(10000).setIdleTimeout(30))
    
    suspend fun extractAndMap(text: String, parentDocument: Document): List<Any> {
        val baseUrl = config.getString("llm.base-url", "http://localhost:8080/v1")
        val glinerModel = config.getString("gliner.model", System.getenv("GLINER_MODEL") ?: "gliner-bi-v2")
        
        // Target specific concepts useful for knowledge graphs
        val systemPrompt = "Extract named entities: CONCEPT, LAW, ORGANIZATION, PERSON, LOCATION, DATE, WEAPON, TACTIC, MISSION, UNIT."
        
        val payload = JsonObject()
            .put("model", glinerModel)
            .put("stream", false)
            .put("messages", JsonArray()
                .add(JsonObject().put("role", "system").put("content", systemPrompt))
                .add(JsonObject().put("role", "user").put("content", text))
            )

        val entities = mutableListOf<Any>()
        try {
            val response = webClient.postAbs("$baseUrl/chat/completions")
                .timeout(60000)
                .sendJsonObject(payload)
                .await()

            if (response.statusCode() == 200) {
                val responseJson = response.bodyAsJsonObject()
                val choices = responseJson.getJsonArray("choices")
                if (choices != null && !choices.isEmpty) {
                    val content = choices.getJsonObject(0).getJsonObject("message").getString("content", "")
                    
                    // GLiNER returns something like "Entity1, Entity2". We tokenize it naively.
                    // This is simple parsing because GLiNER output might be comma-separated or newline-separated.
                    val extractedTerms = content.split(",", "\n", ";").map { it.trim() }.filter { it.isNotBlank() && it.length > 2 }
                    
                    for (term in extractedTerms) {
                        // For MVP, map everything to Concept since GLiNER output is flat text
                        val concept = Concept(name = term)
                        parentDocument.concepts.add(concept)
                        entities.add(concept)
                    }
                }
            } else {
                logger.error("GLiNER Extraction failed with status: ${response.statusCode()} ${response.bodyAsString()}")
            }
        } catch (e: Exception) {
            logger.error("Failed to call GLiNER API", e)
        }
        
        return entities
    }
}
