package com.tactorder.rdss.agent

import com.tactorder.rdss.domain.Person
import com.tactorder.rdss.domain.Law
import com.tactorder.rdss.domain.Concept
import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.service.UserMessage
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory

class CuratorAgent(private val llm: ChatLanguageModel) {
    private val logger = LoggerFactory.getLogger(CuratorAgent::class.java)

    interface CuratorService {
        @UserMessage("""
            You are a Knowledge Graph Curator. 
            Analyze the following list of extracted entities and identify those that refer to the same real-world object.
            
            Entities:
            {{entities}}
            
            Return a JSON object:
            {
              "merged_people": [{"canonical_name": "Jane Doe", "variants": ["Jane Doe", "Commander Doe"], "rank": "Commander"}],
              "merged_laws": [{"canonical_number": "104-A", "variants": ["104-A", "Directive 104-A"]}],
              "merged_concepts": [{"canonical_name": "Drone Protocol", "variants": ["Drone Protocol", "Protocol"]}]
            }
        """)
        fun curate(entities: String): String
    }

    private val service = dev.langchain4j.service.AiServices.create(CuratorService::class.java, llm)

    fun process(entities: JsonObject): JsonObject {
        logger.info("Curator agent canonicalizing entities...")
        
        return try {
            val responseText = service.curate(entities.encodePrettily())
            JsonObject(responseText)
        } catch (e: Exception) {
            logger.error("Failed to curate entities", e)
            entities // Fallback to raw entities
        }
    }
}
