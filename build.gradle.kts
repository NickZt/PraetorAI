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
        add("implementation", platform("io.vertx:vertx-stack-depchain:4.5.8"))
        add("implementation", "io.vertx:vertx-core")
        add("implementation", "io.vertx:vertx-lang-kotlin")
        add("implementation", "io.vertx:vertx-lang-kotlin-coroutines")

        add("implementation", "io.github.microutils:kotlin-logging-jvm:3.0.5")
        add("implementation", "ch.qos.logback:logback-classic:1.4.14")

        add("testImplementation", "io.vertx:vertx-junit5")
        add("testImplementation", "org.junit.jupiter:junit-jupiter:5.10.1")
        add("testImplementation", "io.mockk:mockk:1.13.10")
        add("testImplementation", "org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = "17"
            freeCompilerArgs = listOf("-Xjsr305=strict")
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
