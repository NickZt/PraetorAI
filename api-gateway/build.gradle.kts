plugins {
    application
}

application {
    mainClass.set("io.vertx.core.Launcher")
}

dependencies {
    implementation(project(":core-domain"))
    implementation(libs.vertx.web)
    implementation(libs.vertx.config)
    implementation(libs.jackson.module.kotlin)
}
