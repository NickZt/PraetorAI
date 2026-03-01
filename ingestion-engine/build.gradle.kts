plugins {
    application
}

val mainVerticleName = "com.tactorder.rdss.ingestion.IngestionVerticle"

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
    implementation(libs.vertx.web) // Sometimes needed for transitive deps
    implementation(libs.kotlinx.coroutines.core)
    implementation("io.vertx:vertx-lang-kotlin-coroutines:4.5.8")
    implementation(libs.jackson.module.kotlin)
    implementation(libs.langchain4j.open.ai)
    implementation(libs.neo4j.ogm)
    implementation(libs.neo4j.driver)
    
    // Tika & PDFBox
    implementation("org.apache.tika:tika-core:2.9.2")
    implementation("org.apache.tika:tika-parsers-standard-package:2.9.2")
}
