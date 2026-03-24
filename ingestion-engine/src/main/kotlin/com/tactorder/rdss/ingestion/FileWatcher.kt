package com.tactorder.rdss.ingestion

import io.vertx.core.Vertx
import io.vertx.core.eventbus.EventBus
import org.slf4j.LoggerFactory
import java.nio.file.Paths

class FileWatcher(
    private val vertx: Vertx,
    private val config: io.vertx.core.json.JsonObject
) {
    private val logger = LoggerFactory.getLogger(FileWatcher::class.java)
    private val eventBus: EventBus = vertx.eventBus()

    private val processedFiles = mutableMapOf<String, Long>()

    fun start() {
        val ingestPath = config.getString("ingestion.path", "data/ingest")
        val absolutePath = Paths.get(ingestPath).toAbsolutePath().toString()

        logger.info("Starting Stateful FileWatcher on: $absolutePath")

        vertx.setPeriodic(5000) {
            vertx.fileSystem().readDir(ingestPath) { result ->
                if (result.succeeded()) {
                    result.result().forEach { filePath ->
                        if (!filePath.endsWith(".tmp") && !filePath.endsWith(".processed")) {
                            val file = java.io.File(filePath)
                            val lastModified = file.lastModified()
                            val previousModified = processedFiles[filePath]

                            if (previousModified == null || lastModified > previousModified) {
                                logger.info("Found new or modified file: $filePath (Last Modified: $lastModified)")
                                eventBus.publish("ingestion.new_file", filePath)
                                processedFiles[filePath] = lastModified
                            }
                        }
                    }
                    
                    // Cleanup missing files
                    val currentFiles = result.result().toSet()
                    processedFiles.keys.removeIf { !currentFiles.contains(it) }
                } else {
                    logger.warn("Failed to read ingest dir: ${result.cause().message}")
                }
            }
        }
    }
}
