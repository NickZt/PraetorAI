package com.tactorder.rdss.rag


import com.tactorder.rdss.config.ConfigLoader
import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.openai.OpenAiChatModel
import dev.langchain4j.model.openai.OpenAiEmbeddingModel

import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.launch
import org.neo4j.driver.AuthTokens
import org.neo4j.driver.Driver
import org.neo4j.driver.GraphDatabase
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDate

class RagVerticle : CoroutineVerticle() {

    private val logger = LoggerFactory.getLogger(RagVerticle::class.java)
    private lateinit var neo4jDriver: Driver
    private lateinit var vectorSearch: VectorSearch
    private lateinit var graphRetriever: GraphRetriever
    private lateinit var contextBuilder: ContextBuilder
    private lateinit var chatModel: ChatLanguageModel

    override suspend fun start() {
        logger.info("Starting RagVerticle...")
        
        val configLoader = ConfigLoader(vertx)
        val config = configLoader.loadConfig()
        
        // Neo4j Driver
        val uri = config.getString("neo4j.uri", "bolt://localhost:7687")
        val user = config.getString("neo4j.username", "neo4j")
        val password = config.getString("neo4j.password", "password")
        neo4jDriver = GraphDatabase.driver(uri, AuthTokens.basic(user, password))
        
        // LangChain4j Components - Use Gateway for embeddings
        val embeddingModel = OpenAiEmbeddingModel.builder()
            .baseUrl(config.getString("llm.base-url", "http://localhost:8080/v1"))
            .modelName(config.getString("llm.model", "default"))
            .apiKey("sk-local")
            .timeout(Duration.ofSeconds(30))
            .build()
        
        vectorSearch = VectorSearch(neo4jDriver, embeddingModel)
        graphRetriever = GraphRetriever(neo4jDriver)
        contextBuilder = ContextBuilder()
        
        chatModel = OpenAiChatModel.builder()
            .baseUrl(config.getString("llm.base-url", "http://localhost:8080/v1"))
            .modelName(config.getString("llm.model", "default"))
            .apiKey("sk-local")
            .timeout(Duration.ofSeconds(60))
            .build()

        // EventBus Consumer for RAG Queries
        vertx.eventBus().consumer<JsonObject>("rag.query") { message ->
            launch(vertx.dispatcher()) {
                try {
                    val query = message.body().getString("query")
                    val dateStr = message.body().getString("date")
                    val queryDate = if (dateStr != null) LocalDate.parse(dateStr) else null
                    
                    val response = executeRagPipeline(query, queryDate)
                    message.reply(JsonObject().put("answer", response))
                } catch (e: Exception) {
                    logger.error("RAG pipeline failed", e)
                    message.fail(500, e.message)
                }
            }
        }
        
        logger.info("RagVerticle started and listening on 'rag.query'")
    }
    
    private fun executeRagPipeline(query: String, date: LocalDate?): String {
        // 1. Vector Search
        val vectorResults = vectorSearch.search(query, limit = 5)
        val nodeIds = vectorResults.map { it.getLong("id") }
        
        if (nodeIds.isEmpty()) return "No relevant information found in the knowledge base."
        
        // 2. Graph Traversal (2-hop)
        val graphContext = graphRetriever.retrieveContext(nodeIds, depth = 2)
        
        // 3. Temporal Filtering
        val filteredContext = graphRetriever.temporalFilter(graphContext, date)
        
        // 4. Build Context
        val contextText = contextBuilder.buildContext(filteredContext)
        
        // 5. LLM Generation
        val prompt = """
            You are an expert legal and military research assistant.
            Use the following context from the knowledge base to answer the user's question.
            If the context is insufficient, state that you don't know based on the available data.
            
            Question: $query
            
            $contextText
            
            Answer:
        """.trimIndent()
        
        return chatModel.generate(prompt)
    }

    override suspend fun stop() {
        neo4jDriver.close()
    }
}
