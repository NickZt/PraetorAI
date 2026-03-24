package com.tactorder.rdss.agent

import com.tactorder.rdss.config.ConfigLoader
import io.vertx.core.AbstractVerticle
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

class OrchestratorVerticle : AbstractVerticle() {
    private val logger = LoggerFactory.getLogger(OrchestratorVerticle::class.java)

    private lateinit var curator: CuratorAgent
    private lateinit var scout: ScoutAgent
    private lateinit var advisor: AdvisorAgent
    private lateinit var composer: ComposerAgent
    private lateinit var vision: VisionAgent
    private lateinit var acoustic: AcousticAgent

    override fun start() {
        val configLoader = ConfigLoader(vertx)

        CoroutineScope(vertx.dispatcher()).launch {
            val appConfig = configLoader.loadConfig()

            // Initialize LLM
            val llm = com.tactorder.rdss.config.ModelFactory.createChatModel(appConfig)

            // Initialize Agents
            curator = CuratorAgent(llm)
            scout = ScoutAgent(llm)
            advisor = AdvisorAgent(llm)
            composer = ComposerAgent(llm)
            vision = VisionAgent(appConfig)
            acoustic = AcousticAgent(appConfig)

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
        val payload = data.getJsonObject("payload") ?: data
        return when (task) {
            "curate" -> curator.process(payload)
            "scout" -> scout.scout(payload.getString("query"), payload.getJsonObject("context"))
            "audit" -> advisor.analyze(payload.getJsonObject("new_directive"), payload.getJsonObject("context"))
            "compose" -> JsonObject().put(
                "answer",
                composer.synthesize(payload.getString("query"), payload.getString("context"))
            )

            "vision" -> vision.analyzeImage(payload)
            "audio" -> acoustic.transcribe(payload)
            else -> {
                logger.warn("Unknown task type: $task")
                JsonObject().put("error", "Unknown task type: $task")
            }
        }
    }
}
