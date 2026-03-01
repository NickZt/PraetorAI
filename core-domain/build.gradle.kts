plugins {
    `java-library`
}

dependencies {
    implementation(libs.neo4j.ogm)
    implementation(libs.neo4j.driver)
    implementation(libs.jackson.module.kotlin)
    api(libs.langchain4j.core) // Expose LangChain types
    
    // Config Loader Dependencies
    implementation("io.vertx:vertx-core:4.5.8")
    implementation("io.vertx:vertx-config:4.5.8")
    implementation("io.vertx:vertx-config-yaml:4.5.8")
    implementation("io.vertx:vertx-lang-kotlin-coroutines:4.5.8")
    implementation("io.github.cdimascio:dotenv-java:3.0.0")
}
