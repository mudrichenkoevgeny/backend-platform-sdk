plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.java.library)
}

dependencies {
    // Shared Foundation
    api(libs.shared.foundation.core.common)

    // Kotlin
    api(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.core) // Transitive for Shared Foundation, Ktor, kotlinx-serialization
    api(libs.kotlinx.coroutines.core) // Transitive for Ktor
    implementation(libs.kotlinx.io.core) // Transitive for Ktor
    implementation(libs.kotlin.reflect) // Transitive for Ktor

    // Ktor
    api(libs.ktor.server.core)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.serialization.json)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.default.headers.jvm)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.server.rate.limit)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.core) // Transitive for Ktor
    implementation(libs.ktor.http) // Transitive for Ktor
    implementation(libs.ktor.serialization) // Transitive for Ktor
    implementation(libs.ktor.utils) // Transitive for Ktor
    implementation(libs.ktor.websockets) // Transitive for Ktor

    // DI
    ksp(libs.dagger.compiler)
    api(libs.dagger)
    api(libs.javax.inject) // Transitive for dagger

    // Swagger
    implementation(libs.smiley4.ktor.openapi)
    implementation(libs.smiley4.ktor.swagger.ui)
    implementation(libs.smiley4.schema.kenerator.core)
    implementation(libs.smiley4.schema.kenerator.serialization)
    implementation(libs.smiley4.schema.kenerator.swagger)

    // Logging
    runtimeOnly(libs.logback)
    runtimeOnly(libs.logstash)
    api(libs.slf4j) // Transitive for Ktor, Swagger

    // Testing
    testRuntimeOnly(libs.kotlin.test.junit5)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.mockk)
}

tasks.test {
    useJUnitPlatform()
}

dependencyAnalysis {
    issues {
        onUnusedDependencies {
            exclude(
                "io.github.smiley4:schema-kenerator-core",
                "io.github.smiley4:schema-kenerator-serialization",
                "io.github.smiley4:schema-kenerator-swagger"
            )
        }
    }
}