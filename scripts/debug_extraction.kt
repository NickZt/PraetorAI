import com.tactorder.rdss.rag.RagVerticle
import com.tactorder.rdss.config.ConfigLoader
import com.tactorder.rdss.ingestion.LLMExtractor
import dev.langchain4j.model.openai.OpenAiChatModel
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import java.time.Duration

fun main() {
    val vertx = Vertx.vertx()
    val configLoader = ConfigLoader(vertx)
    val config = configLoader.loadConfig()

    val chatModel = OpenAiChatModel.builder()
        .baseUrl(config.getString("llm.chat.url", "http://localhost:8080/v1"))
        .apiKey("sk-local")
        .modelName(config.getString("llm.chat.model", "native-Qwen3-VL-4B-Instruct-Eagle3-MNN"))
        .timeout(Duration.ofSeconds(180))
        .build()

    val extractor = LLMExtractor(chatModel)
    val text = """
        HEADQUARTERS FIELD DIRECTIVE 104-B
        Date: 2024-05-10
        Subject: Autonomous Drone Deployment Protocol
        Protocol: This supersedes Directive 104-A. All Alpha-Class Drones must now maintain a maximum altitude of 400 feet.
    """.trimIndent()

    println("Extracting...")
    val extracted = extractor.extract(text)
    println("Raw JSON: ${extracted.rawJson}")
    println("Metadata: ${extracted.metadata}")
}
