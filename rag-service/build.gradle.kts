plugins {
    application
}

val mainVerticleName = "com.tactorder.rdss.rag.RagVerticle"

application {
    mainClass.set("io.vertx.core.Launcher")
}

tasks.named<JavaExec>("run") {
    args("run", mainVerticleName)
}

dependencies {
    implementation(project(":core-domain"))
    implementation(libs.vertx.mongo)
    implementation(libs.vertx.config)
    implementation("io.vertx:vertx-config-yaml:4.5.8")
    implementation(libs.vertx.core)
    implementation(libs.kotlinx.coroutines.core)
    implementation("io.vertx:vertx-lang-kotlin-coroutines:4.5.8")
    implementation(libs.langchain4j.open.ai)
    implementation(libs.neo4j.driver)
    implementation(project(":ingestion-engine"))
}
