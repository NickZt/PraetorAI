package com.tactorder.rdss.api

import com.tactorder.rdss.config.ConfigLoader
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.WebClientOptions
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import kotlinx.coroutines.delay

class TelegramBotVerticle : CoroutineVerticle() {

    private val logger = LoggerFactory.getLogger(TelegramBotVerticle::class.java)
    private lateinit var webClient: WebClient
    private lateinit var token: String
    private var lastUpdateId: Long = 0

    override suspend fun start() {
        val config = ConfigLoader(vertx).loadConfig()
        token = config.getString("telegram.bot.token", System.getenv("TELEGRAM_BOT_TOKEN") ?: "")
        
        if (token.isBlank()) {
            logger.warn("Telegram bot token not configured. Telegram bot will not start.")
            return
        }

        logger.info("Starting TelegramBotVerticle...")
        webClient = WebClient.create(vertx, WebClientOptions().setConnectTimeout(5000))
        
        launch(vertx.dispatcher()) {
            pollUpdates()
        }
    }

    private suspend fun pollUpdates() {
        while (true) {
            try {
                val response = webClient.getAbs("https://api.telegram.org/bot$token/getUpdates?offset=$lastUpdateId&timeout=60")
                    .send().await()
                
                if (response.statusCode() == 200) {
                    val json = response.bodyAsJsonObject()
                    if (json.getBoolean("ok") == true) {
                        val results = json.getJsonArray("result")
                        for (i in 0 until results.size()) {
                            val update = results.getJsonObject(i)
                            val updateId = update.getLong("update_id")
                            lastUpdateId = updateId + 1

                            val message = update.getJsonObject("message")
                            if (message != null) {
                                val text = message.getString("text")
                                val chatId = message.getJsonObject("chat")?.getLong("id")
                                
                                if (!text.isNullOrBlank() && chatId != null) {
                                    handleMessage(chatId, text)
                                }
                            }
                        }
                    }
                } else {
                    logger.error("Failed to fetch updates from Telegram: ${response.statusCode()} ${response.bodyAsString()}")
                }
            } catch (e: Exception) {
                logger.error("Error polling Telegram updates", e)
            }
            
            // Short delay to avoid flooding in case of error, though timeout=60 does long polling
            delay(1000)
        }
    }

    private suspend fun handleMessage(chatId: Long, text: String) {
        logger.info("Received message from chat $chatId: $text")
        
        // Indicate typing
        sendChatAction(chatId, "typing")
        
        try {
            // Forward to RAG via EventBus
            val queryBody = JsonObject().put("query", text)
            val result = vertx.eventBus().request<JsonObject>("rag.query", queryBody).coAwait()
            
            // Result is expected to have 'answer' or similar
            val responseBody = result.body()
            val answer = responseBody.getString("answer") ?: responseBody.getString("result") ?: "I processed your request, but the result format was unexpected."
            
            sendMessage(chatId, answer)
        } catch (e: Exception) {
            logger.error("Failed to process RAG query", e)
            sendMessage(chatId, "Sorry, I encountered an error while processing your request: ${e.message}")
        }
    }

    private suspend fun sendChatAction(chatId: Long, action: String) {
        try {
            val payload = JsonObject().put("chat_id", chatId).put("action", action)
            webClient.postAbs("https://api.telegram.org/bot$token/sendChatAction")
                .sendJsonObject(payload).await()
        } catch (e: Exception) {
            logger.warn("Failed to send chat action", e)
        }
    }

    private suspend fun sendMessage(chatId: Long, text: String) {
        try {
            val payload = JsonObject()
                .put("chat_id", chatId)
                .put("text", text)
                .put("parse_mode", "Markdown")
            
            webClient.postAbs("https://api.telegram.org/bot$token/sendMessage")
                .sendJsonObject(payload).await()
        } catch (e: Exception) {
            logger.error("Failed to send message to Telegram", e)
        }
    }
}
