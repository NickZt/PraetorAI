plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.plugin.serialization) apply false
}

group = "com.tactorder.rdss"
version = "0.1.0"

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    dependencies {
        implementation(platform("io.vertx:vertx-stack-depchain:4.5.8"))
        implementation("io.vertx:vertx-core")
        implementation("io.vertx:vertx-lang-kotlin")
        implementation("io.vertx:vertx-lang-kotlin-coroutines")
        
        implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
        implementation("ch.qos.logback:logback-classic:1.4.14")
        
        testImplementation("io.vertx:vertx-junit5")
        testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
        testImplementation("io.mockk:mockk:1.13.10")
        testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = "21"
            freeCompilerArgs = listOf("-Xjsr305=strict")
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
