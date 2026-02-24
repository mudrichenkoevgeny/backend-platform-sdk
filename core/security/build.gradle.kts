plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ksp)
    alias(libs.plugins.java.library)
}

dependencies {
    // Project Modules
    api(project(":core:common"))
    api(project(":core:database"))
    api(project(":core:settings"))

    // Shared Foundation
    implementation(libs.shared.foundation.core.common)
    api(libs.shared.foundation.core.security)

    // Kotlin
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.core) // Transitive for Shared Foundation, Ktor, kotlinx-serialization

    // Ktor
    api(libs.ktor.server.core) // Transitive for Project Modules
    api(libs.ktor.http) // Transitive for Ktor
    implementation(libs.ktor.utils) // Transitive for Ktor

    // DI
    ksp(libs.dagger.compiler)
    api(libs.dagger)
    api(libs.javax.inject) // Transitive for dagger

    // Swagger
    implementation(libs.smiley4.ktor.openapi)

    // Auth & Security
    implementation(libs.password4j)

    // Testing
    testRuntimeOnly(libs.kotlin.test.junit5)
}

tasks.test {
    useJUnitPlatform()
}