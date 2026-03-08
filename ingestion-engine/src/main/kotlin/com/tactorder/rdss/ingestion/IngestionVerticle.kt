package com.tactorder.rdss.ingestion

import com.tactorder.rdss.config.ConfigLoader
import com.tactorder.rdss.domain.Document
import dev.langchain4j.model.openai.OpenAiChatModel
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.CoroutineVerticle
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import java.time.Duration

class IngestionVerticle : CoroutineVerticle() {

    private val logger = LoggerFactory.getLogger(IngestionVerticle::class.java)

    override suspend fun start() {
        logger.info("Starting IngestionVerticle...")

        val configLoader = ConfigLoader(vertx)
        val config = configLoader.loadConfig()

        logger.info("Loaded config from IngestionVerticle: ${config.encodePrettily()}")

        // Initialize Pipeline Components
        val contentExtractor = ContentExtractor()
        val graphWriter = GraphWriter(config)
        val semanticChunker = SemanticChunker(
            chunkSize = config.getInteger("ingestion.chunk-size", 1000),
            overlap = config.getInteger("ingestion.chunk-overlap", 200)
        )

        // Setup Chat Model for LLMExtractor
        val chatModel = OpenAiChatModel.builder().baseUrl(config.getString("llm.base-url", "http://localhost:8080/v1"))
            .modelName(config.getString("llm.chat.model", "native-qwen2.5-7b")).apiKey("sk-local")
            .timeout(Duration.ofSeconds(60)).build()

        val llmExtractor = LLMExtractor(chatModel)

        // Start FileWatcher
        val fileWatcher = FileWatcher(vertx, config)
        fileWatcher.start()

        val ingestionMutex = Mutex()

        // Setup EventBus Listener for New Files
        vertx.eventBus().consumer<String>("ingestion.new_file") { message ->
            val filePath = message.body()
            logger.info("Received new file event for: $filePath")

            launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    ingestionMutex.withLock {
                        processFile(filePath, contentExtractor, semanticChunker, llmExtractor, graphWriter)
                    }
                    message.reply(JsonObject().put("status", "success"))
                } catch (e: Exception) {
                    logger.error("Failed to process file: $filePath", e)
                    message.fail(500, e.message)
                }
            }
        }

        logger.info("IngestionVerticle started successfully.")
    }

    private suspend fun processFile(
        filePath: String,
        contentExtractor: ContentExtractor,
        semanticChunker: SemanticChunker,
        llmExtractor: LLMExtractor,
        graphWriter: GraphWriter
    ) {
        // 1. Extract Text
        logger.info("Extracting content from $filePath")
        val extractedDoc = contentExtractor.extract(filePath)

        // 2. Initial Document Save
        val documentNode = Document(
            title = extractedDoc.fileName, rawText = extractedDoc.content, uri = extractedDoc.filePath
        )
        graphWriter.saveEntities(listOf(documentNode))
        logger.info("Saved base Document node: ${documentNode.title}")

        // 3. Semantic Chunking
        val chunks = semanticChunker.chunk(extractedDoc.content)
        logger.info("Document chunked into ${chunks.size} parts.")

        // 4. LLM Extraction & Mapping
        for ((index, chunk) in chunks.withIndex()) {
            logger.info("Processing chunk ${index + 1}/${chunks.size} through LLMExtractor")
            try {
                val extractedEntities = llmExtractor.extract(chunk)

                // Map the JSON structure to Domain Entities
                val mappedEntities = llmExtractor.mapToDomainEntities(extractedEntities, documentNode)

                // 5. Save to Graph
                if (mappedEntities.isNotEmpty()) {
                    graphWriter.saveEntities(mappedEntities)
                    logger.info("Saved ${mappedEntities.size} mapped entities from chunk ${index + 1} to Graph.")
                }
            } catch (e: Exception) {
                logger.error("Error processing chunk $index", e)
            }
        }

        // Final update to save relations on the document itself
        graphWriter.saveEntities(listOf(documentNode))
        logger.info("Finished processing $filePath")
    }
}
