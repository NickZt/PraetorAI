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

    fun start() {
        val ingestPath = config.getString("ingestion.path", "data/ingest")
        val absolutePath = Paths.get(ingestPath).toAbsolutePath().toString()

        logger.info("Starting FileWatcher on: $absolutePath")

        // Periodically poll or use native watcher if supported/reliable. 
        // Vert.x doesn't have a high-level recursive watcher out of box that is 100% reliable across OS 
        // without some boilerplate, but let's use a periodic timer to scan for new files 
        // OR use the blocking watch service API carefully.
        // For simplicity and robustness in a fog node (often linux), specific OS events are good, 
        // but a polling interval is often safer for "drop file" scenarios to ensure write completion.
        
        // Let's use a simple periodic scan for now to avoid "file still writing" issues, 
        // or just use a standard WatchService in a blocking thread.
        
        // Actually, let's use a vertx timer to scan "new" files.
        // Implementation detail: Keep track of processed files? 
        // Or move them to "processed" folder?
        // Moving is safer.
        
        vertx.setPeriodic(5000) {
            vertx.fileSystem().readDir(ingestPath) { result ->
                if (result.succeeded()) {
                    result.result().forEach { filePath ->
                        // processing logic: usually move to "processing" then fire event
                        // For MVP: just fire event and assume idempotent or move logic is handled by consumer?
                        // Better: Move to 'staging' and then fire.
                        
                        if (!filePath.endsWith(".tmp") && !filePath.endsWith(".processed")) {
                           logger.info("Found new file: $filePath")
                           eventBus.publish("ingestion.new_file", filePath)
                        }
                    }
                } else {
                    logger.warn("Failed to read ingest dir: ${result.cause().message}")
                }
            }
        }
    }
}
