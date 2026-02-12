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
    implementation(libs.vertx.web) // Sometimes needed for transitive deps
    implementation(libs.kotlinx.coroutines.core)
    implementation("io.vertx:vertx-lang-kotlin-coroutines:4.5.8")
    implementation(libs.langchain4j.ollama)
    implementation(libs.neo4j.ogm)
    implementation(libs.neo4j.driver)
    
    // Tika & PDFBox
    implementation("org.apache.tika:tika-core:2.9.2")
    implementation("org.apache.tika:tika-parsers-standard-package:2.9.2")
    implementation("org.apache.pdfbox:pdfbox:3.0.2")
}
