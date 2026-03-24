package com.tactorder.rdss.rag


import com.tactorder.rdss.config.ConfigLoader
import dev.langchain4j.model.openai.OpenAiChatModel
import dev.langchain4j.model.openai.OpenAiEmbeddingModel
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.coAwait
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
    private lateinit var configLoader: ConfigLoader
    private lateinit var appConfig: JsonObject

    override suspend fun start() {
        logger.info("Starting RagVerticle...")

        configLoader = ConfigLoader(vertx)
        appConfig = configLoader.loadConfig()

        // Neo4j Driver
        val uri = appConfig.getString("neo4j.uri", "bolt://localhost:7687")
        val user = appConfig.getString("neo4j.username", "neo4j")
        val password = appConfig.getString("neo4j.password", System.getenv("NEO4J_PASSWORD") ?: "password")
        neo4jDriver = GraphDatabase.driver(uri, AuthTokens.basic(user, password))

        graphRetriever = GraphRetriever(neo4jDriver)
        contextBuilder = ContextBuilder()

        // EventBus Consumer for RAG Queries
        vertx.eventBus().consumer<JsonObject>("rag.query") { message ->
            launch(kotlinx.coroutines.Dispatchers.IO) {
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

        // EventBus Consumer for Graph Statistics
        vertx.eventBus().consumer<JsonObject>("graph.stats") { message ->
            launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val stats = getGraphStatistics()
                    message.reply(stats)
                } catch (e: Exception) {
                    logger.error("Failed to fetch graph stats", e)
                    message.fail(500, e.message)
                }
            }
        }

        // EventBus Consumer for Graph Visualization
        vertx.eventBus().consumer<JsonObject>("graph.visualize") { message ->
            launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val graphData = graphRetriever.getWholeGraph()
                    message.reply(JsonObject().put("data", JsonArray(graphData as List<*>)))
                } catch (e: Exception) {
                    logger.error("Graph visualization failed", e)
                    message.fail(500, e.message)
                }
            }
        }

        logger.info("RagVerticle started and listening on 'rag.query', 'graph.stats' and 'graph.visualize'")
    }

    private suspend fun getGraphStatistics(): JsonObject {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            neo4jDriver.session().use { session ->
                val docs = session.run("MATCH (n:Document) RETURN count(n) as docs").single().get("docs").asLong()
                val concepts = session.run("MATCH (n:Concept) RETURN count(n) as concepts").single().get("concepts").asLong()
                val sections = session.run("MATCH (n:Section) RETURN count(n) as sections").single().get("sections").asLong()
                val relations = session.run("MATCH ()-[r]->() RETURN count(r) as relations").single().get("relations").asLong()
                
                JsonObject()
                    .put("documents", docs)
                    .put("concepts", concepts)
                    .put("sections", sections)
                    .put("relations", relations)
            }
        }
    }

    private suspend fun executeRagPipeline(query: String, date: LocalDate?): String {
        // Init LLM context lazily to avoid race conditions with LLM Gateway startup
        val apiKey = appConfig.getString("llm.api.key", System.getenv("LLM_API_KEY") ?: "sk-local")
        val embeddingModel = OpenAiEmbeddingModel.builder()
            .baseUrl(appConfig.getString("llm.base-url", "http://localhost:8080/v1"))
            .modelName(appConfig.getString("llm.embedding.model", "native-Qwen3-Embedding-4B-MNN"))
            .apiKey(apiKey)
            .timeout(Duration.ofSeconds(appConfig.getString("llm.timeout", "180").toLong()))
            .build()

        vectorSearch = VectorSearch(neo4jDriver, embeddingModel)

        val chatModel = OpenAiChatModel.builder()
            .baseUrl(appConfig.getString("llm.base-url", "http://localhost:8080/v1"))
            .modelName(appConfig.getString("llm.chat.model", "native-Qwen3-VL-4B-Instruct-Eagle3-MNN"))
            .apiKey(apiKey)
            .timeout(Duration.ofSeconds(appConfig.getString("llm.timeout", "180").toLong()))
            .build()

        // 1. Vector Search
        val vectorResults = vectorSearch.search(query, limit = 5)
        
        val nodeIds = vectorResults.map { it.getLong("id") }

        if (nodeIds.isEmpty()) return "No relevant information found in the knowledge base."

        // 2. Graph Traversal (Adaptive Depth with Scout Agent)
        logger.info("Requesting Scout investigation for query: $query")
        val scoutRequest = JsonObject()
            .put("task", "scout")
            .put("query", query)
            .put("context", JsonObject().put("ids", JsonArray(nodeIds)))
        
        // Scout might suggest deeper traversal or specific relationship follows
        val scoutResponse = try {
            vertx.eventBus().request<JsonObject>("agent.orchestrate", scoutRequest).coAwait().body()
        } catch (e: Exception) {
            JsonObject().put("hop_required", false)
        }

        val depth = if (scoutResponse.getBoolean("hop_required", false)) 3 else 2
        val graphContext = graphRetriever.retrieveContext(nodeIds, depth = depth, queryDate = date)

        // 3. Build Context
        val contextText = contextBuilder.buildContext(graphContext)
      //  logger.info("TODEL RAG Context built for query '$query':\n$contextText")

        // 4. Agent Orchestration (Composer) for Final Synthesis
        val prompt = "Question: $query\nContext: $contextText"
        val composerRequest = JsonObject()
            .put("task", "compose")
            .put("query", query)
            .put("context", contextText)
        
        logger.info("Requesting synthesis from Composer agent...")
        return try {
            val composerResponse = vertx.eventBus().request<JsonObject>("agent.orchestrate", composerRequest).coAwait().body()
            composerResponse.getString("answer")
        } catch (e: Exception) {
            logger.error("Composer agent failed, falling back to manual prompt", e)
            chatModel.generate(prompt)
        }
    }

    override suspend fun stop() {
        neo4jDriver.close()
    }
}
