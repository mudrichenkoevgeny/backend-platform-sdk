plugins {
    alias(libs.plugins.kotlin.jvm)
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
    testRuntimeOnly(libs.kotlin.test.junit5)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.core) // Transitive for Project Modules
}

tasks.test {
    useJUnitPlatform()
}