package com.tactorder.rdss.ingestion

import com.tactorder.rdss.config.ConfigLoader
import com.tactorder.rdss.domain.*
import dev.langchain4j.model.openai.OpenAiChatModel
import dev.langchain4j.model.openai.OpenAiEmbeddingModel
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.coAwait
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
            chunkSize = config.getJsonObject("ingestion")?.getInteger("chunk-size") ?: 1000,
            overlap = config.getJsonObject("ingestion")?.getInteger("chunk-overlap") ?: 200
        )

        val chatModel = OpenAiChatModel.builder()
            .baseUrl(config.getJsonObject("llm")?.getString("base-url") ?: "http://localhost:8080/v1")
            .modelName(config.getJsonObject("llm")?.getJsonObject("chat")?.getString("model") ?: "native-Qwen3-VL-4B-Instruct-Eagle3-MNN")
            .apiKey(config.getJsonObject("llm")?.getString("api.key") ?: System.getenv("LLM_API_KEY") ?: "sk-local")
            .timeout(Duration.ofSeconds(config.getJsonObject("llm")?.getString("timeout")?.toLong() ?: 180L))
            .build()
        
        val embeddingModel = OpenAiEmbeddingModel.builder()
            .baseUrl(config.getJsonObject("llm")?.getString("base-url") ?: "http://localhost:8080/v1")
            .modelName(config.getJsonObject("llm")?.getJsonObject("embedding")?.getString("model") ?: "native-Qwen3-Embedding-4B-MNN")
            .apiKey(config.getJsonObject("llm")?.getString("api.key") ?: "sk-local")
            .timeout(Duration.ofSeconds(config.getJsonObject("llm")?.getString("timeout")?.toLong() ?: 180L))
            .build()

        val chunkSize = config.getJsonObject("ingestion")?.getInteger("chunk-size") ?: 1000
        val chunkOverlap = config.getJsonObject("ingestion")?.getInteger("chunk-overlap") ?: 200
        val extractionMode = config.getJsonObject("extraction")?.getString("mode") ?: "llm"
        
        val llmExtractor = LLMExtractor(chatModel)
        val glinerExtractor = GlinerExtractor(vertx, config)

        // Start FileWatcher
        val fileWatcher = FileWatcher(vertx, config)
        fileWatcher.start()

        val ingestionMutex = Mutex()

        // Setup EventBus Listener for New Files (Multi-Modal Dispatcher)
        vertx.eventBus().consumer<String>("ingestion.new_file") { message ->
            val filePath = message.body()
            val extension = File(filePath).extension.lowercase()
            logger.info("Received new file event for: $filePath (Type: $extension)")

            launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    ingestionMutex.withLock {
                        when (extension) {
                            "jpg", "jpeg", "png" -> processImageFile(filePath, graphWriter)
                            "mp3", "wav" -> processAudioFile(filePath, graphWriter)
                            else -> processTextFile(filePath, contentExtractor, semanticChunker, llmExtractor, glinerExtractor, graphWriter, embeddingModel, extractionMode)
                        }
                    }
                    message.reply(JsonObject().put("status", "success"))
                } catch (e: Exception) {
                    logger.error("Failed to process multi-modal file: $filePath", e)
                    message.fail(500, e.message)
                }
            }
        }

        logger.info("IngestionVerticle started successfully.")
    }

    private suspend fun processTextFile(
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
        val fileBytes = file.readBytes()
        val md5Hash = MessageDigest.getInstance("MD5").digest(fileBytes).joinToString("") { "%02x".format(it) }
        val fileSize = file.length()

        if (graphWriter.isDocumentIngested(md5Hash)) {
            logger.warn("Document $filePath (MD5: $md5Hash) already ingested. Skipping.")
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
                    val mapped = llmExtractor.mapToDomainEntities(extractedEntities, documentNode)
                    
                    // Automated Supersedes Linking
                    val targetNum = extractedEntities.metadata["supersedes"]
                    if (targetNum != null) {
                        val targetLaw = graphWriter.findLawByNumber(targetNum)
                        if (targetLaw != null) {
                            logger.info("Found supersedes target: Law $targetNum. Linking...")
                            mapped.filterIsInstance<Law>().forEach { currentLaw ->
                                currentLaw.supersedes.add(targetLaw)
                            }
                        } else {
                            logger.warn("Supersedes target Law $targetNum not found in DB yet.")
                        }
                    }
                    val mappedList = mapped.toMutableList()

                    // 4.5. Agent Orchestration (Curator) for post-processing
                    try {
                        val orchestrateRequest = JsonObject()
                            .put("task", "curate")
                            .put("payload", JsonObject().put("entities", JsonArray(mappedList.map { JsonObject.mapFrom(it) })))
                        
                        logger.info("Requesting curation from Agent Orchestrator...")
                        val curatorResponse = vertx.eventBus().request<JsonObject>(
                            "agent.orchestrate", 
                            orchestrateRequest,
                            io.vertx.core.eventbus.DeliveryOptions().setSendTimeout(180000)
                        ).coAwait().body()
                        logger.info("Curator response: ${curatorResponse.getString("status", "processed")}")
                        // In future: update mappedList based on curatorResponse
                    } catch (e: Exception) {
                        logger.warn("Agent Orchestration failed, proceeding with raw entities: ${e.message}")
                    }

                    mappedList
                }

                // 5. Save to Graph (including Chunk, Document, and mapped entities)
                val allEntities = mutableListOf<Any>(chunkNode, documentNode)
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

        // 6. Proactive Advisory Audit (Agent Orchestrator)
        val auditRequest = JsonObject()
            .put("task", "audit")
            .put("new_directive", JsonObject().put("id", documentNode.id).put("title", documentNode.title))
            .put("context", JsonObject())
        
        logger.info("Triggering Proactive Advisor audit for ${documentNode.title}...")
        vertx.eventBus().send("agent.orchestrate", auditRequest)
    }

    private suspend fun processImageFile(filePath: String, graphWriter: GraphWriter) {
        val file = File(filePath)
        val fileBytes = file.readBytes()
        val md5Hash = MessageDigest.getInstance("MD5").digest(fileBytes).joinToString("") { "%02x".format(it) }
        
        logger.info("Processing Image: $filePath (MD5: $md5Hash)")
        
        // 1. Trigger VisionAgent for Captioning
        val captionRequest = JsonObject()
            .put("task", "vision")
            .put("payload", JsonObject().put("image_path", filePath).put("task", "caption"))
        
        val captionResponse = vertx.eventBus().request<JsonObject>("agent.orchestrate", captionRequest).coAwait().body()
        val description = captionResponse.getString("analysis", "No caption generated.")

        // 2. Trigger VisionAgent for OCR
        val ocrRequest = JsonObject()
            .put("task", "vision")
            .put("payload", JsonObject().put("image_path", filePath).put("task", "ocr"))
        
        val ocrResponse = vertx.eventBus().request<JsonObject>("agent.orchestrate", ocrRequest).coAwait().body()
        val ocrText = ocrResponse.getString("analysis", "")

        // 3. Save to Graph
        val imageNode = Image(
            name = file.name,
            uri = filePath,
            description = description,
            ocrText = ocrText,
            md5Hash = md5Hash
        )
        graphWriter.saveEntities(listOf(imageNode))
        logger.info("Saved Image node: ${imageNode.name} with MD5: $md5Hash")
    }

    private suspend fun processAudioFile(filePath: String, graphWriter: GraphWriter) {
        val file = File(filePath)
        val fileBytes = file.readBytes()
        val md5Hash = MessageDigest.getInstance("MD5").digest(fileBytes).joinToString("") { "%02x".format(it) }
        
        logger.info("Processing Audio: $filePath (MD5: $md5Hash)")
        
        // Audio processing (AcousticAgent) will be implemented in Step 2 of Phase 5
        val audioNode = Audio(
            name = file.name,
            uri = filePath,
            transcript = "Pending transcription (AcousticAgent)",
            md5Hash = md5Hash
        )
        graphWriter.saveEntities(listOf(audioNode))
        logger.info("Saved stub Audio node: ${audioNode.name}")
    }
}
