plugins {
    application
}

val mainVerticleName = "com.tactorder.rdss.agent.OrchestratorVerticle"

application {
    mainClass.set("io.vertx.core.Launcher")
}

tasks.named<JavaExec>("run") {
    args("run", mainVerticleName)
}

dependencies {
    implementation(project(":core-domain"))
    implementation(libs.vertx.config)
    implementation("io.vertx:vertx-config-yaml:4.5.8")
    implementation(libs.jackson.module.kotlin)
    implementation(libs.langchain4j.open.ai)
    implementation(libs.langchain4j.core)
    implementation(libs.neo4j.driver)
    implementation(libs.neo4j.ogm)
}
