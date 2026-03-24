package com.tactorder.rdss.ingestion

import com.tactorder.rdss.config.ConfigLoader
import com.tactorder.rdss.config.ModelFactory
import com.tactorder.rdss.domain.Audio
import com.tactorder.rdss.domain.Chunk
import com.tactorder.rdss.domain.Document
import com.tactorder.rdss.domain.Image
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

        val chatModel = ModelFactory.createChatModel(config)
        val embeddingModel = ModelFactory.createEmbeddingModel(config)

        val llmExtractor = LLMExtractor(chatModel)
        val glinerExtractor = GlinerExtractor(vertx, config)
        val extractionMode = config.getJsonObject("extraction")?.getString("mode") ?: "llm"

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
                            else -> processTextFile(
                                filePath,
                                contentExtractor,
                                semanticChunker,
                                llmExtractor,
                                glinerExtractor,
                                graphWriter,
                                embeddingModel,
                                extractionMode
                            )
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


    private fun getMd5Hash(file: File): String {
        return MessageDigest.getInstance("MD5").digest(file.readBytes()).joinToString("") { "%02x".format(it) }
    }

    private fun checkDeduplication(md5Hash: String, graphWriter: GraphWriter): Boolean {
        if (graphWriter.isDocumentIngested(md5Hash)) {
            logger.warn("Content with MD5: $md5Hash already ingested. Skipping.")
            return true
        }
        return false
    }

    private suspend fun requestAgent(task: String, payload: JsonObject): JsonObject {
        val request = JsonObject().put("task", task).put("payload", payload)
        return try {
            vertx.eventBus().request<JsonObject>(
                "agent.orchestrate",
                request,
                io.vertx.core.eventbus.DeliveryOptions().setSendTimeout(180000)
            ).coAwait().body()
        } catch (e: Exception) {
            logger.error("Agent request failed: $task", e)
            JsonObject().put("status", "error").put("message", e.message)
        }
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
        val md5Hash = getMd5Hash(file)
        if (checkDeduplication(md5Hash, graphWriter)) return

        val fileSize = file.length()
        // ... (remaining extraction logic is the same)
        logger.info("Extracting content from $filePath")
        val extractedDoc = contentExtractor.extract(filePath)

        val documentNode = Document(
            title = extractedDoc.fileName,
            rawText = extractedDoc.content,
            uri = extractedDoc.filePath,
            md5Hash = md5Hash,
            fileSize = fileSize
        )
        graphWriter.saveEntities(listOf(documentNode))

        val chunks = semanticChunker.chunk(extractedDoc.content)
        for ((index, chunkText) in chunks.withIndex()) {
            try {
                val vector = embeddingModel.embed(chunkText).content().vector().map { it.toDouble() }.toDoubleArray()
                val chunkNode = Chunk(text = chunkText, embedding = vector, index = index)

                val entitiesToSave = if (extractionMode == "gliner") {
                    glinerExtractor.extractAndMap(chunkText, documentNode)
                } else {
                    val extractedEntities = llmExtractor.extract(chunkText)
                    val mapped = llmExtractor.mapToDomainEntities(extractedEntities, documentNode)
                    val mappedList = mapped.toMutableList()

                    val curatorResponse = requestAgent(
                        "curate",
                        JsonObject().put("entities", JsonArray(mappedList.map { JsonObject.mapFrom(it) }))
                    )
                    logger.info("Curator status: ${curatorResponse.getString("status", "processed")}")

                    mappedList
                }

                val allEntities = mutableListOf<Any>(chunkNode, documentNode)
                allEntities.addAll(entitiesToSave)
                graphWriter.saveEntities(allEntities)
            } catch (e: Exception) {
                logger.error("Error processing chunk $index", e)
            }
        }
        graphWriter.saveEntities(listOf(documentNode))

        // Audit
        requestAgent(
            "audit",
            JsonObject().put("new_directive", JsonObject().put("id", documentNode.id).put("title", documentNode.title))
                .put("context", JsonObject())
        )
    }

    private suspend fun processImageFile(filePath: String, graphWriter: GraphWriter) {
        val file = File(filePath)
        val md5Hash = getMd5Hash(file)
        if (checkDeduplication(md5Hash, graphWriter)) return

        logger.info("Processing Image: $filePath")

        val captionResponse = requestAgent("vision", JsonObject().put("image_path", filePath).put("task", "caption"))
        val ocrResponse = requestAgent("vision", JsonObject().put("image_path", filePath).put("task", "ocr"))

        val imageNode = Image(
            name = file.name,
            uri = filePath,
            description = captionResponse.getString("analysis", "No caption."),
            ocrText = ocrResponse.getString("analysis", ""),
            md5Hash = md5Hash
        )
        graphWriter.saveEntities(listOf(imageNode))
        logger.info("Saved Image node: ${imageNode.name}")
    }

    private suspend fun processAudioFile(filePath: String, graphWriter: GraphWriter) {
        val file = File(filePath)
        val md5Hash = getMd5Hash(file)
        if (checkDeduplication(md5Hash, graphWriter)) return

        logger.info("Processing Audio: $filePath")

        val audioResponse = requestAgent("audio", JsonObject().put("audio_path", filePath))

        val audioNode = Audio(
            name = file.name,
            uri = filePath,
            transcript = audioResponse.getString("transcript", "No transcript."),
            md5Hash = md5Hash
        )
        graphWriter.saveEntities(listOf(audioNode))
        logger.info("Saved Audio node: ${audioNode.name}")
    }
}
