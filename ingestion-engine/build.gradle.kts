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
    
    // Tika & PDFBox (manual versions as they are specific)
    implementation("org.apache.tika:tika-core:2.9.2")
    implementation("org.apache.tika:tika-parsers-standard-package:2.9.2")
    implementation("org.apache.pdfbox:pdfbox:3.0.2")
}
