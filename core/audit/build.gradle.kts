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
    api(platform(libs.shared.foundation.bom))
    api(libs.shared.foundation.core.common)

    // Kotlin
    api(platform(libs.kotlinx.serialization.bom))
    api(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.core) // Transitive for Shared Foundation, Ktor, kotlinx-serialization
    api(libs.kotlinx.coroutines.core) // Transitive for Ktor

    // Ktor
    implementation(platform(libs.ktor.bom))
    implementation(libs.ktor.server.core)

    // DI
    ksp(libs.dagger.compiler)
    api(libs.dagger)
    api(libs.javax.inject) // Transitive for dagger

    // Database
    api(platform(libs.exposed.bom))
    api(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.json)

    // Testing
    testRuntimeOnly(libs.kotlin.test.junit5)
}

tasks.test {
    useJUnitPlatform()
}