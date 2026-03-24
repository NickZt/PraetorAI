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

    fun analyze(newDirective: JsonObject, context: JsonObject): JsonObject {
        logger.info("Advisor agent auditing new directive...")
        // We will implement the evaluation logic in the next steps
        return JsonObject().put("status", "safe")
    }
}
