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
    implementation(libs.langchain4j.ollama)
    implementation(libs.langchain4j.neo4j)
}
