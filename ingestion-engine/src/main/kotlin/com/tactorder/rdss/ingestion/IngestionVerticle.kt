package com.tactorder.rdss.ingestion

import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import org.slf4j.LoggerFactory

class IngestionVerticle : CoroutineVerticle() {

    private val logger = LoggerFactory.getLogger(IngestionVerticle::class.java)

    override suspend fun start() {
        logger.info("Starting IngestionVerticle...")
        
        val configLoader = ConfigLoader(vertx)
        val config = configLoader.loadConfig()
        
        logger.info("Loaded config: {}", config.encodePrettily())

        // Placeholder for File Watcher logic
        // val watchDir = config.getString("ingestion.path", "./data/ingest")
        // startFileWatcher(watchDir)
        
        logger.info("IngestionVerticle started successfully.")
    }
}
