package com.tactorder.rdss.ingestion

import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.await

class ConfigLoader(private val vertx: Vertx) {

    suspend fun loadConfig(): JsonObject {
        val envStore = ConfigStoreOptions()
            .setType("env")

        val fileStore = ConfigStoreOptions()
            .setType("file")
            .setFormat("yaml")
            .setConfig(JsonObject().put("path", "conf/config.yaml"))
            .setOptional(true)

        val options = ConfigRetrieverOptions()
            .addStore(fileStore)
            .addStore(envStore)

        val retriever = ConfigRetriever.create(vertx, options)
        return retriever.config.await()
    }
}
