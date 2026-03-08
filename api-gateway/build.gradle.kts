plugins {
    application
}

val mainVerticleName = "com.tactorder.rdss.api.ApiVerticle"

application {
    mainClass.set("io.vertx.core.Launcher")
}

tasks.named<JavaExec>("run") {
    args("run", mainVerticleName)
}

dependencies {
    implementation(project(":core-domain"))
    implementation(libs.vertx.web)
    implementation(libs.vertx.config)
    implementation(libs.vertx.core)
    implementation(libs.kotlinx.coroutines.core)
    implementation("io.vertx:vertx-lang-kotlin-coroutines:4.5.8")
    implementation("io.vertx:vertx-web-client:4.5.8")
    implementation(libs.jackson.module.kotlin)
}
