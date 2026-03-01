package com.tactorder.rdss

import com.tactorder.rdss.api.ApiVerticle
import com.tactorder.rdss.api.TelegramBotVerticle
import com.tactorder.rdss.ingestion.IngestionVerticle
import com.tactorder.rdss.rag.RagVerticle
import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import io.vertx.core.Vertx
import org.slf4j.LoggerFactory

class MainLauncher : AbstractVerticle() {
    private val logger = LoggerFactory.getLogger(MainLauncher::class.java)

    override fun start(startPromise: Promise<Void>) {
        logger.info("Starting RDSS Main Launcher...")

        deployHelper(LlmManagerVerticle())
            .compose { deployHelper(IngestionVerticle()) }
            .compose { deployHelper(RagVerticle()) }
            .compose { deployHelper(ApiVerticle()) }
            .compose { deployHelper(TelegramBotVerticle()) }
            .onSuccess { 
                logger.info("All components started successfully.")
                startPromise.complete()
            }
            .onFailure { 
                logger.error("Failed to start components", it)
                startPromise.fail(it)
            }
    }

    private fun deployHelper(verticle: io.vertx.core.Verticle): io.vertx.core.Future<String> {
        return vertx.deployVerticle(verticle)
    }
}

fun main() {
    val vertx = Vertx.vertx()
    vertx.deployVerticle(MainLauncher())
}
