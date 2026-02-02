plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ksp)
    alias(libs.plugins.java.library)
}

dependencies {
    // Project Modules
    api(project(":core:common"))

    // Shared Foundation
    api(platform(libs.shared.foundation.bom))
    api(libs.shared.foundation.core.common)

    // Kotlin
    implementation(libs.kotlinx.coroutines.core) // Transitive for Ktor

    // Ktor
    api(platform(libs.ktor.bom))
    api(libs.ktor.server.core) // Transitive for Project Modules, ktor-server-metrics-micrometer
    implementation(libs.ktor.server.metrics.micrometer)
    implementation(libs.ktor.http) // Transitive for Ktor
    implementation(libs.ktor.utils) // Transitive for Ktor

    // DI
    ksp(libs.dagger.compiler)
    api(libs.dagger)
    api(libs.javax.inject) // Transitive for dagger

    // Swagger
    implementation(libs.smiley4.ktor.openapi)

    // Logging
    implementation(libs.slf4j) // Transitive for Project Modules, Ktor

    // Observability
    api(platform(libs.opentelemetry.bom))
    api(libs.opentelemetry.api)
    runtimeOnly(libs.opentelemetry.exporter.otlp)
    implementation(libs.opentelemetry.extension.kotlin)
    api(libs.micrometer.registry.prometheus)
    implementation(libs.opentelemetry.context) // Transitive for Opentelemetry
    implementation(libs.micrometer.core) // Transitive for ktor-server-metrics-micrometer

    // Testing
    testRuntimeOnly(libs.kotlin.test.junit5)
}

tasks.test {
    useJUnitPlatform()
}