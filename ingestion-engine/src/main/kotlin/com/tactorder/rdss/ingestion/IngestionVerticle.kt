package com.tactorder.rdss.ingestion

import com.tactorder.rdss.config.ConfigLoader
import com.tactorder.rdss.domain.*
import dev.langchain4j.model.openai.OpenAiChatModel
import dev.langchain4j.model.openai.OpenAiEmbeddingModel
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.CoroutineVerticle
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import java.io.File
import java.security.MessageDigest
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

        val chatModel = OpenAiChatModel.builder()
            .baseUrl(config.getString("llm.base-url", "http://localhost:8080/v1"))
            .modelName(config.getString("llm.chat.model", "onnx-Qwen2.5-0.5B-Instruct-ONNX"))
            .apiKey(config.getString("llm.api.key", System.getenv("LLM_API_KEY") ?: "sk-local"))
            .timeout(Duration.ofSeconds(config.getString("llm.timeout", "180").toLong()))
            .build()
        
        val embeddingModel = OpenAiEmbeddingModel.builder()
            .baseUrl(config.getString("llm.base-url", "http://localhost:8080/v1"))
            .modelName(config.getString("llm.embedding.model", "native-Qwen3-Embedding-4B-MNN"))
            .apiKey(config.getString("llm.api.key", "sk-local"))
            .timeout(Duration.ofSeconds(config.getString("llm.timeout", "180").toLong()))
            .build()

        val llmExtractor = LLMExtractor(chatModel)
        val glinerExtractor = GlinerExtractor(vertx, config)

        val extractionMode = config.getString("extraction.mode", System.getenv("EXTRACTION_MODE") ?: "llm")

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
                    processFile(filePath, contentExtractor, semanticChunker, llmExtractor, glinerExtractor, graphWriter, embeddingModel, extractionMode)
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
        glinerExtractor: GlinerExtractor,
        graphWriter: GraphWriter,
        embeddingModel: OpenAiEmbeddingModel,
        extractionMode: String
    ) {
        val file = File(filePath)
        if (!file.exists()) {
            logger.error("File does not exist: $filePath")
            return
        }

        val fileBytes = file.readBytes()
        val md5Hash = MessageDigest.getInstance("MD5").digest(fileBytes).joinToString("") { "%02x".format(it) }
        val fileSize = file.length()

        if (graphWriter.isDocumentIngested(md5Hash)) {
            logger.warn("Document $filePath (MD5: $md5Hash) already ingested. Skipping deduplication.")
            return
        }

        // 1. Extract Text
        logger.info("Extracting content from $filePath")
        val extractedDoc = contentExtractor.extract(filePath)

        // 2. Initial Document Save
        val documentNode = Document(
            title = extractedDoc.fileName, rawText = extractedDoc.content, uri = extractedDoc.filePath, md5Hash = md5Hash, fileSize = fileSize
        )
        graphWriter.saveEntities(listOf(documentNode))
        logger.info("Saved base Document node: ${documentNode.title} with Hash: $md5Hash Size: $fileSize bytes")

        // 3. Semantic Chunking
        val chunks = semanticChunker.chunk(extractedDoc.content)
        logger.info("Document chunked into ${chunks.size} parts.")

        // 4. LLM Extraction & Mapping
        for ((index, chunkText) in chunks.withIndex()) {
            logger.info("Processing chunk ${index + 1}/${chunks.size}")
            try {
                // Compute Embedding
                val vector = embeddingModel.embed(chunkText).content().vector().map { it.toDouble() }.toDoubleArray()
                val chunkNode = Chunk(text = chunkText, embedding = vector, index = index)
                documentNode.chunks.add(chunkNode)
                
                val entitiesToSave = if (extractionMode == "gliner") {
                    logger.info("Extracting chunk through GLiNER...")
                    glinerExtractor.extractAndMap(chunkText, documentNode)
                } else {
                    logger.info("Extracting chunk through LLM Qwen Chat...")
                    val extractedEntities = llmExtractor.extract(chunkText)
                    llmExtractor.mapToDomainEntities(extractedEntities, documentNode)
                }

                // 5. Save to Graph (including Chunk and mapped entities)
                val allEntities = mutableListOf<Any>(chunkNode)
                allEntities.addAll(entitiesToSave)
                
                graphWriter.saveEntities(allEntities)
                logger.info("Saved chunk ${index + 1} and ${entitiesToSave.size} mapped entities to Graph.")
            } catch (e: Exception) {
                logger.error("Error processing chunk $index", e)
            }
        }

        // Final update to save relations on the document itself
        graphWriter.saveEntities(listOf(documentNode))
        logger.info("Finished processing $filePath")
    }
}
