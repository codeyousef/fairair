plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

dependencies {
    // Spring Boot WebFlux
    implementation(libs.spring.boot.starter.webflux)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.security)
    
    // Spring Data R2DBC for reactive database access
    implementation(libs.spring.boot.starter.data.r2dbc)
    
    // H2 Database with R2DBC (file-based mode for persistence)
    // To swap to PostgreSQL: replace with r2dbc-postgresql
    implementation(libs.r2dbc.h2)
    runtimeOnly(libs.h2.database)

    // Kotlin
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.reactor)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)

    // Jackson
    implementation(libs.jackson.module.kotlin)

    // Caffeine (caching)
    implementation(libs.caffeine)

    // Google Cloud Vertex AI
    implementation(libs.google.cloud.vertexai)
    
    // Google Cloud Speech-to-Text and Text-to-Speech
    implementation(libs.google.cloud.speech)
    implementation(libs.google.cloud.texttospeech)

    // JWT
    implementation(libs.jjwt.api)
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)

    // Rate Limiting
    implementation(libs.bucket4j.core)

    // Shared Contract
    implementation(project(":shared-contract"))

    // Testing
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.kotlin.test)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        freeCompilerArgs.addAll(
            "-Xjsr305=strict",
            "-opt-in=kotlin.RequiresOptIn"
        )
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
