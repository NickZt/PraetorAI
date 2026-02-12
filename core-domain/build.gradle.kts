plugins {
    `java-library`
}

dependencies {
    implementation(libs.neo4j.ogm)
    implementation(libs.neo4j.driver)
    implementation(libs.jackson.module.kotlin)
    api(libs.langchain4j.core) // Expose LangChain types
}
