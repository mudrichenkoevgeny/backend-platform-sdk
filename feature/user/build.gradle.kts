plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.java.library)
    `java-test-fixtures`
}

dependencies {
    // Project Modules
    api(project(":core:common"))
    api(project(":core:database"))
    api(project(":core:security"))
    api(project(":core:audit"))
    api(project(":core:settings"))

    // Shared Foundation
    api(libs.shared.foundation.core.common)
    api(libs.shared.foundation.core.settings)
    api(libs.shared.foundation.core.security)
    api(libs.shared.foundation.feature.user)

    // Kotlin
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.core) // Transitive for Shared Foundation, Ktor, kotlinx-serialization
    implementation(libs.kotlinx.coroutines.core) // Transitive for Ktor
    implementation(libs.kotlin.reflect) // Transitive for Ktor

    // Ktor
    api(libs.ktor.server.core)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.http) // Transitive for Ktor
    implementation(libs.ktor.utils) // Transitive for Ktor
    implementation(libs.ktor.websockets) // Transitive for Ktor
    implementation(libs.java.jwt) // Transitive for ktor-server-auth-jwt

    // DI
    ksp(libs.dagger.compiler)
    api(libs.dagger)
    api(libs.javax.inject) // Transitive for dagger

    // Database
    api(libs.exposed.core)
    runtimeOnly(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.java.time)
    implementation(libs.exposed.json)

    // Swagger
    implementation(libs.smiley4.ktor.openapi)

    // Logging
    implementation(libs.slf4j) // Transitive for Ktor, Swagger

    // Auth & Security
    api(libs.jjwt.api)
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)
    implementation(libs.google.auth.oauth2.http)
    implementation(libs.google.http.client)

    // Testing
    testFixturesApi(libs.ktor.serialization.json)
    testFixturesApi(libs.mockk)
    testFixturesApi(libs.ktor.server.status.pages)
    testFixturesApi(libs.ktor.server.content.negotiation)
    testFixturesApi(libs.ktor.server.test.host)
    testFixturesApi(libs.ktor.server.core)
    testFixturesApi(libs.ktor.server.auth)
    testFixturesApi(libs.ktor.server.auth.jwt)
    testFixturesApi(libs.ktor.client.content.negotiation)

    testRuntimeOnly(libs.kotlin.test.junit5)
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.h2database)
    testImplementation(libs.ktor.server.test.host)
}

tasks.test {
    useJUnitPlatform()
}