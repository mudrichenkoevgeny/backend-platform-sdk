plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.java.library)
}

dependencies {
    // Project Modules
    api(project(":core:common"))
    api(project(":core:database"))

    // Shared Foundation
    implementation(libs.shared.foundation.core.common)
    api(libs.shared.foundation.core.settings)

    // Ktor
    runtimeOnly(libs.ktor.server.core) // Transitive for Project Modules

    // DI
    ksp(libs.dagger.compiler)
    api(libs.dagger)
    api(libs.javax.inject) // Transitive for dagger

    // Database
    api(libs.exposed.core)
    runtimeOnly(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    runtimeOnly(libs.postgresql)
    runtimeOnly(libs.flyway.postgresql)

    // Swagger
    implementation(libs.smiley4.ktor.openapi)

    // Logging
    runtimeOnly(libs.logback)

    // Testing
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.kotlin.test.junit5)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testRuntimeOnly(libs.h2database)
}

tasks.test {
    useJUnitPlatform()
}