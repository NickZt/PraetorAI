package com.tactorder.rdss

import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.WebClientOptions
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import java.io.File

class LlmManagerVerticle : CoroutineVerticle() {
    private val logger = LoggerFactory.getLogger(LlmManagerVerticle::class.java)
    private lateinit var client: WebClient

    override suspend fun start() {
        val options = WebClientOptions().setConnectTimeout(2000)
        client = WebClient.create(vertx, options)

        logger.info("Verifying LLM Gateway Status...")
        val isUp = checkLlmHealth()

        if (!isUp) {
            logger.warn("LLM Gateway is unreachable. Attempting to start MNNLLama...")
            startLlmProcess()

            // Wait for it to become healthy
            var retries = 0
            val maxRetries = 15
            var healthy = false

            while (retries < maxRetries && !healthy) {
                delay(2000) // Wait 2 seconds between checks
                healthy = checkLlmHealth()
                retries++
            }

            if (!healthy) {
                val errorMsg = "FATAL: Failed to start and connect to MNNLLama Gateway after $maxRetries attempts."
                logger.error(errorMsg)
                throw RuntimeException(errorMsg)
            }
        }

        logger.info("LLM Gateway is ONLINE. Verifying loaded models...")
        verifyModels()
    }

    private suspend fun checkLlmHealth(): Boolean {
        return try {
            val response = client.getAbs("http://localhost:8080/v1/models").send().await()
            response.statusCode() == 200
        } catch (e: Exception) {
            false
        }
    }

    private fun startLlmProcess() {
        // Find the mnn-llm executable path - assuming it's built in the workspace root or accessible via PATH
        val projectRoot = System.getProperty("user.dir").replace("/bootstrap", "")

        // This command assumes MNN-LLM is built and located somewhere known, e.g., a bin folder or the mnn-service build dir
        // For demonstration, we assume there's a script `start-mnn.sh` or similar. 
        // If the exact path is unknown, we will use a placeholder or check common paths.
        val expectedGatewayPath = "/home/nickzt/Projects/TactOrder/MNNLLama/gateway"
        val gatewayProjectDir = File(expectedGatewayPath)

        if (!gatewayProjectDir.exists() || !File(gatewayProjectDir, "build.gradle.kts").exists()) {
            logger.error("Could not find MNN LLM Gateway project at $expectedGatewayPath")
            logger.warn("Please ensure MNN Gateway is checked out or start it manually.")
            // Don't crash immediately, let the retry loop fail on connection which is standard
            return
        }

        try {
            logger.info("Launching MNNLLama Gateway via Gradle from $expectedGatewayPath")
            val jniPath = "/home/nickzt/Projects/TactOrder/MNNLLama/inference-services/mnn-service/build"
            val pb = ProcessBuilder("./gradlew", "run")

            pb.directory(gatewayProjectDir)

            // Pass the JNI library path using JAVA_OPTS
            val env = pb.environment()
            env["JAVA_OPTS"] = "-Djava.library.path=\$jniPath"

            pb.redirectOutput(ProcessBuilder.Redirect.appendTo(File("$projectRoot/logs/mnn.log").apply { parentFile.mkdirs() }))
            pb.redirectErrorStream(true)
            val p = pb.start()
            logger.info("Started Gateway process list with PID: \${p.pid()}")
        } catch (e: Exception) {
            logger.error("Failed to execute LLM process", e)
        }
    }

    private suspend fun verifyModels() {
        try {
            val response = client.getAbs("http://localhost:8080/v1/models").send().await()
            if (response.statusCode() == 200) {
                val body = response.bodyAsJsonObject()
                val data = body.getJsonArray("data") ?: io.vertx.core.json.JsonArray()

                if (data.isEmpty) {
                    logger.warn("MNN Gateway is running, but ZERO models are loaded.")
                } else {
                    val availableModels = data.map { (it as JsonObject).getString("id") }
                    logger.info("Available LLM Models: ${availableModels.joinToString(", ")}")
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to verify models", e)
        }
    }
}
