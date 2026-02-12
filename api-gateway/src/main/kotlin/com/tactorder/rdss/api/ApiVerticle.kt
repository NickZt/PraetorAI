package com.tactorder.rdss.api

import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

class ApiVerticle : CoroutineVerticle() {

    private val logger = LoggerFactory.getLogger(ApiVerticle::class.java)

    override suspend fun start() {
        logger.info("Starting ApiVerticle...")
        
        val router = Router.router(vertx)
        router.route().handler(BodyHandler.create())
        
        router.post("/ingest").handler { ctx ->
            // Forward to Ingestion Service (EventBus)
            ctx.response().end("Ingestion started")
        }
        
        router.post("/query").handler { ctx ->
             // Forward to RAG Service (EventBus)
             ctx.response().end("Query received")
        }
        
        vertx.createHttpServer()
            .requestHandler(router)
            .listen(8080)
            .onSuccess { logger.info("HTTP Server listening on port 8080") }
            .onFailure { logger.error("Failed to start HTTP Server", it) }
    }
}
