plugins {
    application
}

application {
    mainClass.set("io.vertx.core.Launcher")
}

dependencies {
    implementation(project(":core-domain"))
    implementation(libs.vertx.mongo)
    implementation(libs.vertx.config)
    implementation(libs.vertx.core)
    implementation(libs.kotlinx.coroutines.core)
    implementation("io.vertx:vertx-lang-kotlin-coroutines:4.5.8")
    implementation(libs.langchain4j.ollama)
    implementation(libs.neo4j.driver)
    implementation(project(":ingestion-engine"))
}
