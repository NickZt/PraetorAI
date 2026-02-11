package com.tactorder.rdss

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.scheduling.annotation.EnableAsync

@SpringBootApplication
@EnableNeo4jRepositories
@EnableMongoRepositories
@EnableAsync
class RdssApplication

fun main(args: Array<String>) {
    runApplication<RdssApplication>(*args)
}
