package com.tactorder.rdss.agent

import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.service.UserMessage
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory

class ComposerAgent(private val llm: ChatLanguageModel) {
    private val logger = LoggerFactory.getLogger(ComposerAgent::class.java)

    interface ComposerService {
        @UserMessage("""
            You are an expert military drone operations analyst. 
            Synthesize a clear, structured answer to the user's question using the provided context.
            
            Question: {{query}}
            Context: {{context}}
            
            Rules:
            1. Cite specific directives (e.g. 104-A) when possible.
            2. If information is missing, state it clearly.
            3. Use bold text for key limits (e.g. **600 feet**).
            
            Answer:
        """)
        fun compose(query: String, context: String): String
    }

    fun synthesize(query: String, context: String): String {
        logger.info("Composer agent synthesizing answer for: $query")
        // Basic implementation for now
        return llm.generate("Synthesize answer for: $query with context: $context")
    }
}
