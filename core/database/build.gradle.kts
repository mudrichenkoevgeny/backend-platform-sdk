plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ksp)
    alias(libs.plugins.java.library)
}

dependencies {
    // Project Modules
    api(project(":core:common"))
    api(project(":core:observability"))

    // Kotlin
    api(libs.kotlinx.coroutines.core) // Transitive for Project Modules

    // Ktor
    implementation(platform(libs.ktor.bom))
    implementation(libs.ktor.server.core) // Transitive for Project Modules

    // DI
    ksp(libs.dagger.compiler)
    api(libs.dagger)
    api(libs.javax.inject) // Transitive for dagger

    // Database
    api(platform(libs.exposed.bom))
    api(libs.exposed.core)
    runtimeOnly(libs.exposed.dao)
    api(libs.exposed.jdbc)
    implementation(libs.exposed.java.time)
    runtimeOnly(libs.postgresql)
    implementation(libs.hikari)
    implementation(libs.flyway.core)
    runtimeOnly(libs.flyway.postgresql)

    // Logging
    runtimeOnly(libs.logback)

    // Observability
    api(libs.micrometer.registry.prometheus)
    implementation(libs.micrometer.core) // Transitive for Micrometer

    // Infrastructure
    api(libs.lettuce.core)

    // Testing
    testRuntimeOnly(libs.kotlin.test.junit5)
    testImplementation(platform(libs.junit.bom))
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.mockk)
    testRuntimeOnly(libs.h2database)
    testImplementation(libs.kotlinx.coroutines.core) // Transitive for Project Modules
}

tasks.test {
    useJUnitPlatform()
}