package com.tactorder.rdss.agent

import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class AcousticAgent(private val config: JsonObject) {
    private val logger = LoggerFactory.getLogger(AcousticAgent::class.java)

    /**
     * Transcribes an audio file via a Python script fallback (for LangChain4j compatibility).
     * Expects payload: { "audio_path": "..." }
     */
    fun transcribe(payload: JsonObject): JsonObject {
        val audioPath = payload.getString("audio_path")
        val llmConfig = config.getJsonObject("llm") ?: JsonObject()
        val baseUrl = llmConfig.getString("base-url") ?: "http://localhost:8080/v1"
        val apiKey = llmConfig.getString("api.key") ?: System.getenv("LLM_API_KEY") ?: "sk-local"
        val modelName = llmConfig.getJsonObject("audio")?.getString("model") ?: "whisper-1"

        logger.info("AcousticAgent: Transcribing audio from $audioPath via helper script...")

        return try {
            val audioFile = File(audioPath)
            if (!audioFile.exists()) {
                throw IllegalArgumentException("Audio file not found: $audioPath")
            }

            val pb = ProcessBuilder(
                "python3",
                "/home/nickzt/Projects/TactOrder/RDSS/scripts/transcribe.py",
                audioPath, baseUrl, apiKey, modelName
            )
            val process = pb.start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val transcriptList = reader.lines().toList()
            val transcript = transcriptList.joinToString(" ").trim()
            process.waitFor()

            if (process.exitValue() != 0) {
                val errorReader = BufferedReader(InputStreamReader(process.errorStream))
                val errorMsg = errorReader.lines().toList().joinToString(" ")
                throw RuntimeException("Transcription script failed: $errorMsg")
            }

            JsonObject()
                .put("status", "success")
                .put("transcript", transcript)
        } catch (e: Exception) {
            logger.error("AcousticAgent failed: ${e.message}")
            JsonObject()
                .put("status", "error")
                .put("message", e.message)
        }
    }
}
