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
    api(project(":core:security"))
    api(project(":core:audit"))
    api(project(":core:crosscutting"))

    // Shared Foundation
    api(platform(libs.shared.foundation.bom))
    api(libs.shared.foundation.core.common)
    api(libs.shared.foundation.core.security)
    api(libs.shared.foundation.feature.user)

    // Kotlin
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.core) // Transitive for Shared Foundation, Ktor, kotlinx-serialization
    implementation(libs.kotlinx.coroutines.core) // Transitive for Ktor
    implementation(libs.kotlin.reflect) // Transitive for Ktor

    // Ktor
    api(platform(libs.ktor.bom))
    api(libs.ktor.server.core)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.http) // Transitive for Ktor
    implementation(libs.ktor.utils) // Transitive for Ktor
    implementation(libs.java.jwt) // Transitive for ktor-server-auth-jwt

    // DI
    ksp(libs.dagger.compiler)
    api(libs.dagger)
    api(libs.javax.inject) // Transitive for dagger

    // Database
    api(platform(libs.exposed.bom))
    api(libs.exposed.core)
    runtimeOnly(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.java.time)

    // Swagger
    implementation(libs.smiley4.ktor.openapi)

    // Logging
    implementation(libs.slf4j) // Transitive for Ktor, Swagger

    // Auth & Security
    api(platform(libs.jjwt.bom))
    api(libs.jjwt.api)
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)

    // Testing
    testRuntimeOnly(libs.kotlin.test.junit5)
    testImplementation(platform(libs.junit.bom))
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.mockk)
}

tasks.test {
    useJUnitPlatform()
}