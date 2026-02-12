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
    implementation(libs.vertx.core)
    implementation("ch.qos.logback:logback-classic:1.4.14")
}
