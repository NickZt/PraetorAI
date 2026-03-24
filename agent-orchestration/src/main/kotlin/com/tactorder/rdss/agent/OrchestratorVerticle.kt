package com.tactorder.rdss.agent

import com.tactorder.rdss.config.ConfigLoader
import dev.langchain4j.model.openai.OpenAiChatModel
import io.vertx.core.AbstractVerticle
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.time.Duration

class OrchestratorVerticle : AbstractVerticle() {
    private val logger = LoggerFactory.getLogger(OrchestratorVerticle::class.java)
    
    private lateinit var curator: CuratorAgent
    private lateinit var scout: ScoutAgent
    private lateinit var advisor: AdvisorAgent
    private lateinit var composer: ComposerAgent
    private lateinit var vision: VisionAgent

    override fun start() {
        val configLoader = ConfigLoader(vertx)
        
        CoroutineScope(vertx.dispatcher()).launch {
            val appConfig = configLoader.loadConfig()
            
            // Initialize LLM
            val llm = OpenAiChatModel.builder()
                .baseUrl(appConfig.getString("llm.base-url", "http://localhost:8080/v1"))
                .apiKey("sk-local")
                .modelName(appConfig.getString("llm.chat.model", "native-Qwen3-VL-4B-Instruct-Eagle3-MNN"))
                .timeout(Duration.ofSeconds(180))
                .build()

            // Initialize Agents
            curator = CuratorAgent(llm)
            scout = ScoutAgent(llm)
            advisor = AdvisorAgent(llm)
            composer = ComposerAgent(llm)
            vision = VisionAgent(appConfig)
            
            // Listen for orchestration requests
            vertx.eventBus().consumer<JsonObject>("agent.orchestrate") { message ->
                val body = message.body()
                val task = body.getString("task", "unknown")
                logger.info("Received orchestration task: $task")
                
                // Offload blocking LLM calls to IO thread pool
                CoroutineScope(vertx.dispatcher()).launch {
                    try {
                        val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            handleTaskSync(task, body)
                        }
                        message.reply(result)
                    } catch (e: Exception) {
                        logger.error("Orchestration task failed: $task", e)
                        message.fail(500, e.message)
                    }
                }
            }
            
            logger.info("Agent Orchestrator Verticle started with Qwen3 4B.")
        }
    }

    private fun handleTaskSync(task: String, data: JsonObject): JsonObject {
        return when (task) {
            "curate" -> curator.process(data.getJsonObject("payload"))
            "scout" -> scout.scout(data.getString("query"), data.getJsonObject("context"))
            "audit" -> advisor.analyze(data.getJsonObject("new_directive"), data.getJsonObject("context"))
            "compose" -> JsonObject().put("answer", composer.synthesize(data.getString("query"), data.getString("context")))
            "vision" -> vision.analyzeImage(data.getJsonObject("payload"))
            else -> {
                logger.warn("Unknown task type: $task")
                JsonObject().put("error", "Unknown task type: $task")
            }
        }
    }
}
