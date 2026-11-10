plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shared.foundation.quality)
    alias(libs.plugins.shared.foundation.kotlin)
    alias(libs.plugins.shared.foundation.publish)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.java.library)
}

dependencies {
    // Project Modules
    api(project(":core:common"))

    // Shared Foundation
    api(libs.shared.foundation.core.common)

    // Kotlin
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.serialization.core) // Transitive for Shared Foundation, Project Modules, kotlinx-serialization

    // Ktor
    implementation(libs.ktor.server.core) // Transitive for Project Modules

    // DI
    ksp(libs.dagger.compiler)
    api(libs.dagger)
    api(libs.javax.inject) // Transitive for dagger

    // Infrastructure
    implementation(libs.apache.kafka)

    // Testing
    testImplementation(testFixtures(project(":core:common")))
    testRuntimeOnly(libs.kotlin.test.junit5)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.core) // Transitive for Project Modules
    testImplementation(libs.testcontainers.core)
    testImplementation(libs.testcontainers.kafka)
    testImplementation(libs.testcontainers.junit.jupiter)
}

tasks.test {
    useJUnitPlatform()
}
