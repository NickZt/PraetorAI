package com.tactorder.rdss.ingestion

import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.coAwait

class ConfigLoader(private val vertx: Vertx) {

    suspend fun loadConfig(): JsonObject {
        val envStore = ConfigStoreOptions()
            .setType("env")

        val rootPath = java.io.File(System.getProperty("user.dir")).absolutePath
        val isSubproject = rootPath.endsWith("bootstrap") || rootPath.endsWith("ingestion-engine")
        val finalConfigPath = if (isSubproject) "../conf/config.yaml" else "conf/config.yaml"

        val fileStore = ConfigStoreOptions()
            .setType("file")
            .setFormat("yaml")
            .setConfig(JsonObject().put("path", finalConfigPath))
            .setOptional(true)

        val options = ConfigRetrieverOptions()
            .addStore(fileStore)
            .addStore(envStore)

        val retriever = ConfigRetriever.create(vertx, options)
        return retriever.config.coAwait()
    }
}
