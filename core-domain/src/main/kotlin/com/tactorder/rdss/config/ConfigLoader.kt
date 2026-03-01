package com.tactorder.rdss.config

import io.github.cdimascio.dotenv.Dotenv
import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.coAwait
import org.slf4j.LoggerFactory

class ConfigLoader(private val vertx: Vertx) {
    private val logger = LoggerFactory.getLogger(ConfigLoader::class.java)

    suspend fun loadConfig(): JsonObject {
        // Load .env automatically (if present)
        val dotenv = try {
            Dotenv.configure()
                .directory("/home/nickzt/Projects/TactOrder/RDSS")
                .ignoreIfMissing()
                .load()
        } catch (e: Exception) {
            logger.warn("Could not load .env file from /home/nickzt/Projects/TactOrder/RDSS. Falling back to system environment variables.")
            null
        }

        // 1. Base Configuration (YAML)
        val fileStore = ConfigStoreOptions()
            .setType("file")
            .setFormat("yaml")
            .setConfig(JsonObject().put("path", "/home/nickzt/Projects/TactOrder/RDSS/conf/config.yaml"))
            .setOptional(true)

        // 2. System Environment Variables
        val envStore = ConfigStoreOptions().setType("env")

        val options = ConfigRetrieverOptions()
            .addStore(fileStore)
            .addStore(envStore)

        val retriever = ConfigRetriever.create(vertx, options)
        val baseConfig = retriever.config.coAwait()

        // 3. Merge .env Variables directly into Vert.x JsonObject (highest priority)
        val finalConfig = baseConfig.copy()
        
        dotenv?.entries()?.forEach { entry ->
            val key = entry.key.lowercase().replace("_", ".")
            finalConfig.put(key, entry.value)
        }

        System.getenv().forEach { (k, v) ->
            val key = k.lowercase().replace("_", ".")
            finalConfig.put(key, v)
        }

        return finalConfig
    }
}
