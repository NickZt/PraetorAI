package com.tactorder.rdss.agent

import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.service.UserMessage
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory

class AdvisorAgent(private val llm: ChatLanguageModel) {
    private val logger = LoggerFactory.getLogger(AdvisorAgent::class.java)

    interface AdvisorService {
        @UserMessage("""
            You are a Military Regulatory Advisor. 
            Evaluate the following new directive against existing operational context.
            
            New Directive:
            {{new_directive}}
            
            Existing Context:
            {{existing_context}}
            
            Determine if there is a conflict. 
            Return a JSON object:
            {
              "conflict_found": true/false,
              "severity": "high/medium/low",
              "explanation": "If conflict, explain why...",
              "recommendation": "What should the commander do?"
            }
        """)
        fun evaluateConflict(new_directive: String, existing_context: String): String
    }

    private val service = dev.langchain4j.service.AiServices.create(AdvisorService::class.java, llm)
    
    fun analyze(newDirective: JsonObject, context: JsonObject): JsonObject {
        logger.info("Advisor agent auditing new directive for conflicts...")
        
        val newText = newDirective.getString("text", "")
        val contextText = context.getJsonArray("existing_nodes")?.encodePrettily() ?: "None"
        
        return try {
            val responseText = service.evaluateConflict(newText, contextText)
            // The service returns a JSON string as per the prompt instructions
            JsonObject(responseText)
        } catch (e: Exception) {
            logger.error("Failed to evaluate conflict", e)
            JsonObject().put("conflict_found", false).put("status", "error").put("message", e.message)
        }
    }
}
