plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ksp)
    alias(libs.plugins.java.library)
    `java-test-fixtures`
}

dependencies {
    // Shared Foundation
    api(libs.shared.foundation.core.storage)

    // Project Modules
    api(project(":core:common"))
    api(project(":core:database"))
    api(project(":core:audit"))
    api(project(":core:settings"))

    // Kotlin
    implementation(libs.kotlinx.coroutines.core)

    // Ktor
    implementation(libs.ktor.server.core) // Transitive for Project Modules
    api(libs.ktor.server.auth.jwt)

    // DI
    ksp(libs.dagger.compiler)
    api(libs.dagger)
    api(libs.javax.inject) // Transitive for dagger

    // Swagger
    implementation(libs.smiley4.ktor.openapi)

    // Infrastructure
    implementation(libs.aws.s3)
    runtimeOnly(libs.aws.apache.client)
    implementation(libs.aws.auth) // Transitive for aws
    implementation(libs.aws.core) // Transitive for aws
    implementation(libs.aws.http.client.spi) // Transitive for aws
    implementation(libs.aws.regions) // Transitive for aws
    implementation(libs.aws.sdk.core) // Transitive for aws

    // Testing
    testImplementation(testFixtures(project(":core:common")))
    testRuntimeOnly(libs.h2database)
    testRuntimeOnly(libs.kotlin.test.junit5)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.test {
    useJUnitPlatform()
}