package com.tactorder.rdss.api

import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

class ApiVerticle : CoroutineVerticle() {

    private val logger = LoggerFactory.getLogger(ApiVerticle::class.java)

    override suspend fun start() {
        logger.info("Starting ApiVerticle...")

        val router = Router.router(vertx)
        router.route().handler(BodyHandler.create())

        // CORS Support
        router.route().handler(
            io.vertx.ext.web.handler.CorsHandler.create()
                .addOrigin("*")
                .allowedMethod(io.vertx.core.http.HttpMethod.GET)
                .allowedMethod(io.vertx.core.http.HttpMethod.POST)
                .allowedHeader("Content-Type")
        )

        // Ingestion Endpoint
        router.post("/ingest").handler { ctx ->
            val body = ctx.body().asJsonObject()
            val filePath = body.getString("path")

            if (filePath.isNullOrBlank()) {
                ctx.response().setStatusCode(400).end("Missing 'path' in body")
                return@handler
            }

            // Publish to Ingestion Service (Fire and Forget)
            vertx.eventBus().publish("ingestion.new_file", filePath)
            ctx.response()
                .putHeader("Content-Type", "application/json")
                .end(JsonObject().put("status", "Ingestion started for $filePath").encode())
        }

        // RAG Query Endpoint
        router.post("/query").handler { ctx ->
            val body = ctx.body().asJsonObject()
            val query = body.getString("query")

            if (query.isNullOrBlank()) {
                ctx.response().setStatusCode(400).end("Missing 'query' in body")
                return@handler
            }

            // Request-Reply to RAG Service
            launch(vertx.dispatcher()) {
                try {
                    val result = vertx.eventBus().request<JsonObject>("rag.query", body).coAwait()
                    ctx.response()
                        .putHeader("Content-Type", "application/json")
                        .end(result.body().encode())
                } catch (e: Exception) {
                    logger.error("Query failed", e)
                    ctx.response()
                        .setStatusCode(500)
                        .putHeader("Content-Type", "application/json")
                        .end(JsonObject().put("error", e.message).encode())
                }
            }
        }

        // Graph Visualization Endpoint (Stub for now)
        router.get("/graph/visualize").handler { ctx ->
            // TODO: Fetch graph data from Neo4j directly or via service
            ctx.response().end(JsonObject().put("nodes", listOf<Any>()).put("edges", listOf<Any>()).encode())
        }
        
        // Graph Statistics Endpoint
        router.get("/stats").handler { ctx ->
            launch(vertx.dispatcher()) {
                try {
                    val result = vertx.eventBus().request<JsonObject>("graph.stats", JsonObject()).coAwait()
                    ctx.response()
                        .putHeader("Content-Type", "application/json")
                        .end(result.body().encode())
                } catch (e: Exception) {
                    logger.error("Stats query failed", e)
                    ctx.response()
                        .setStatusCode(500)
                        .putHeader("Content-Type", "application/json")
                        .end(JsonObject().put("error", e.message).encode())
                }
            }
        }

        vertx.createHttpServer()
            .requestHandler(router)
            .listen(8081)
            .onSuccess { logger.info("HTTP Server listening on port 8081") }
            .onFailure { logger.error("Failed to start HTTP Server", it) }
    }
}
