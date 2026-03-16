plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ksp)
    alias(libs.plugins.java.library)
}

dependencies {
    // Project Modules
    api(project(":core:common"))
    api(project(":core:database"))

    // Shared Foundation
    api(libs.shared.foundation.core.common)

    // Kotlin
    api(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.core) // Transitive for Shared Foundation, Ktor, kotlinx-serialization
    api(libs.kotlinx.coroutines.core) // Transitive for Ktor

    // Ktor
    implementation(libs.ktor.server.core)

    // DI
    ksp(libs.dagger.compiler)
    api(libs.dagger)
    api(libs.javax.inject) // Transitive for dagger

    // Database
    api(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.json)

    // Testing
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.kotlin.test.junit5)
    testImplementation(libs.h2database)
}

tasks.test {
    useJUnitPlatform()
}