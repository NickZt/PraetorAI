package com.tactorder.rdss.agent

import dev.langchain4j.model.chat.ChatLanguageModel
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory

class ScoutAgent(private val llm: ChatLanguageModel) {
    private val logger = LoggerFactory.getLogger(ScoutAgent::class.java)

    interface ScoutService {
        @dev.langchain4j.service.UserMessage("""
            You are a Knowledge Graph Scout. 
            Given the user query and the current retrieved context (node IDs and types), determine if a multi-hop traversal is required to find the answer.
            
            Query: {{query}}
            Current Context: {{context}}
            
            Return a JSON object:
            {
              "hop_required": true/false,
              "reason": "Why or why not...",
              "suggested_depth": 2 or 3,
              "target_relationship": "e.g. SUPERSEDES, AMENDS, RELATES_TO"
            }
        """)
        fun planExploration(query: String, context: String): String
    }

    private val service = dev.langchain4j.service.AiServices.create(ScoutService::class.java, llm)

    fun scout(query: String, context: JsonObject): JsonObject {
        logger.info("Scout agent planning exploration for: $query")
        
        return try {
            val responseText = service.planExploration(query, context.encodePrettily())
            JsonObject(responseText)
        } catch (e: Exception) {
            logger.error("Failed to plan exploration", e)
            JsonObject().put("hop_required", false).put("status", "error")
        }
    }
}
