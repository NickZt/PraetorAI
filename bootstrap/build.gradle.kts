plugins {
    application
}

application {
    mainClass.set("com.tactorder.rdss.MainLauncherKt")
}

dependencies {
    implementation(project(":core-domain"))
    implementation(project(":ingestion-engine"))
    implementation(project(":rag-service"))
    implementation(project(":api-gateway"))
    implementation(libs.neo4j.ogm)
    implementation(libs.neo4j.driver)
    implementation("org.neo4j:neo4j-ogm-bolt-driver:4.0.8")
    implementation(libs.vertx.core)
    implementation("io.vertx:vertx-web-client:4.5.8")
    implementation("ch.qos.logback:logback-classic:1.4.14")
}
