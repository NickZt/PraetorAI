package uk.mezon.rdss.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class RagRequest(val query: String, val date: String? = null)

@Serializable
data class RagResponse(val answer: String? = null, val error: String? = null)

@Serializable
data class IngestRequest(val path: String)

@Serializable
data class IngestResponse(val status: String? = null, val error: String? = null)

@Serializable
data class StatsResponse(
    val documents: Long = 0,
    val concepts: Long = 0,
    val sections: Long = 0,
    val relations: Long = 0,
    val error: String? = null
)

class PraetorApiClient(private val baseUrl: String = "http://localhost:8081") {
    
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            })
        }
    }

    suspend fun queryRagAgent(query: String, date: String? = null): RagResponse {
        return try {
            client.post("$baseUrl/query") {
                contentType(ContentType.Application.Json)
                setBody(RagRequest(query = query, date = date?.takeIf { it.isNotBlank() }))
            }.body()
        } catch (e: Exception) {
            RagResponse(error = e.message ?: "Failed to connect to Praetor AI Gateway.")
        }
    }

    suspend fun ingestDocument(path: String): IngestResponse {
        return try {
            client.post("$baseUrl/ingest") {
                contentType(ContentType.Application.Json)
                setBody(IngestRequest(path = path))
            }.body()
        } catch (e: Exception) {
            IngestResponse(error = e.message ?: "Failed to connect to Praetor AI Gateway.")
        }
    }

    suspend fun fetchStats(): StatsResponse {
        return try {
            client.get("$baseUrl/stats").body()
        } catch (e: Exception) {
            StatsResponse(error = e.message ?: "Failed to connect to Praetor AI Gateway.")
        }
    }
}
