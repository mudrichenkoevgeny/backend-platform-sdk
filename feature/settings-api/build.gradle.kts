plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ksp)
    alias(libs.plugins.java.library)
}

dependencies {
    // Project Modules
    api(project(":core:common"))
    api(project(":core:audit"))
    api(project(":core:settings"))
    api(project(":feature:user"))

    // Shared Foundation
    implementation(libs.shared.foundation.core.common)
    api(libs.shared.foundation.core.settings)
    implementation(libs.shared.foundation.core.audit)
    implementation(libs.shared.foundation.feature.settings.api)

    // Ktor
    implementation(libs.ktor.server.auth)

    // DI
    ksp(libs.dagger.compiler)
    api(libs.dagger)
    api(libs.javax.inject) // Transitive for dagger

    // Swagger
    implementation(libs.smiley4.ktor.openapi)

    // Testing
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.kotlinx.coroutines.test)
    testRuntimeOnly(libs.kotlin.test.junit5)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.mockk)
    testRuntimeOnly(libs.h2database)
}

tasks.test {
    useJUnitPlatform()
}
