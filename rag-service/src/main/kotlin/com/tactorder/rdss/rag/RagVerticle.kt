package com.tactorder.rdss.rag


import com.tactorder.rdss.config.ConfigLoader
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.coAwait
import kotlinx.coroutines.launch
import org.neo4j.driver.AuthTokens
import org.neo4j.driver.Driver
import org.neo4j.driver.GraphDatabase
import org.slf4j.LoggerFactory
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
        val uri = appConfig.getJsonObject("neo4j")?.getString("uri") ?: "bolt://localhost:7687"
        val user = appConfig.getJsonObject("neo4j")?.getString("username") ?: "neo4j"
        val password =
            appConfig.getJsonObject("neo4j")?.getString("password") ?: System.getenv("NEO4J_PASSWORD") ?: "password"
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

    private suspend fun requestAgent(task: String, payload: JsonObject): JsonObject {
        val request = JsonObject().put("task", task).put("payload", payload)
        return try {
            vertx.eventBus().request<JsonObject>(
                "agent.orchestrate",
                request,
                io.vertx.core.eventbus.DeliveryOptions().setSendTimeout(180000)
            ).coAwait().body()
        } catch (e: Exception) {
            logger.error("Agent request failed: $task", e)
            JsonObject().put("status", "error").put("message", e.message)
        }
    }

    private suspend fun getGraphStatistics(): JsonObject {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            neo4jDriver.session().use { session ->
                val docs = session.run("MATCH (n:Document) RETURN count(n) as docs").single().get("docs").asLong()
                val concepts =
                    session.run("MATCH (n:Concept) RETURN count(n) as concepts").single().get("concepts").asLong()
                val sections =
                    session.run("MATCH (n:Section) RETURN count(n) as sections").single().get("sections").asLong()
                val relations =
                    session.run("MATCH ()-[r]->() RETURN count(r) as relations").single().get("relations").asLong()

                JsonObject()
                    .put("documents", docs)
                    .put("concepts", concepts)
                    .put("sections", sections)
                    .put("relations", relations)
            }
        }
    }

    private suspend fun executeRagPipeline(query: String, date: LocalDate?): String {
        val chatModel = com.tactorder.rdss.config.ModelFactory.createChatModel(appConfig)
        val embeddingModel = com.tactorder.rdss.config.ModelFactory.createEmbeddingModel(appConfig)
        vectorSearch = VectorSearch(neo4jDriver, embeddingModel)

        // 0. Handle [FASTPATH] Bypass
        val fastPathRegex = Regex("\\[FASTPATH\\]", RegexOption.IGNORE_CASE)
        val isFastPath = fastPathRegex.find(query) != null
        val cleanQuery = query.replace(fastPathRegex, "").trim()

        logger.info("[ORCHESTRATOR] RAG Execution - FastPath: $isFastPath, Original: '$query', Clean: '$cleanQuery'")
        if (isFastPath) logger.info("🚀 FASTPATH detected! Bypassing agentic orchestration.")

        // 1. Vector Search
        val searchLimit = appConfig.getJsonObject("rag")?.getInteger("vector-search-limit") ?: 5
        val vectorResults = vectorSearch.search(cleanQuery, limit = searchLimit)

        val nodeIds = vectorResults.map { it.getString("id") }

        if (nodeIds.isEmpty()) return "No relevant information found in the knowledge base."

        // 2. Graph Traversal (Adaptive vs Base Depth)
        val baseDepth = appConfig.getJsonObject("rag")?.getInteger("graph-traversal-depth") ?: 2
        var depth = baseDepth

        if (!isFastPath) {
            logger.info("Requesting Scout investigation for query: $cleanQuery")
            val scoutResponse = requestAgent(
                "scout",
                JsonObject().put("query", cleanQuery).put("context", JsonObject().put("ids", JsonArray(nodeIds)))
            )
            if (scoutResponse.getBoolean("hop_required", false)) depth += 1
        }

        val graphContext = graphRetriever.retrieveContext(nodeIds, depth = depth, queryDate = date)

        // 3. Build Context
        val contextText = contextBuilder.buildContext(graphContext)
        val prompt = "Question: $cleanQuery\nContext: $contextText"

        // 4. Final Synthesis (Agentic vs Direct)
        return if (!isFastPath) {
            logger.info("Requesting synthesis from Composer agent...")
            val composerResponse =
                requestAgent("compose", JsonObject().put("query", cleanQuery).put("context", contextText))

            if (composerResponse.getString("status") != "error") {
                composerResponse.getString("answer")
            } else {
                logger.error("Composer agent failed, falling back to manual prompt")
                chatModel.generate(prompt)
            }
        } else {
            logger.info("Directly synthesizing FASTPATH response...")
            chatModel.generate(prompt)
        }
    }

    override suspend fun stop() {
        neo4jDriver.close()
    }
}
