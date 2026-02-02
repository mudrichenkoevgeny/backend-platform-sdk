plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ksp)
    alias(libs.plugins.java.library)
}

dependencies {
    // Project Modules
    api(project(":core:common"))

    // Kotlin
    implementation(libs.kotlinx.coroutines.core)

    // Ktor
    implementation(platform(libs.ktor.bom))
    implementation(libs.ktor.server.core) // Transitive for Project Modules

    // DI
    ksp(libs.dagger.compiler)
    api(libs.dagger)
    api(libs.javax.inject) // Transitive for dagger

    // Infrastructure
    implementation(libs.aws.s3)
    runtimeOnly(libs.aws.apache.client)
    implementation(platform(libs.aws.sdk.bom))
    implementation(libs.aws.auth) // Transitive for aws
    implementation(libs.aws.core) // Transitive for aws
    implementation(libs.aws.http.client.spi) // Transitive for aws
    implementation(libs.aws.regions) // Transitive for aws
    implementation(libs.aws.sdk.core) // Transitive for aws

    // Testing
    testRuntimeOnly(libs.kotlin.test.junit5)
}

tasks.test {
    useJUnitPlatform()
}