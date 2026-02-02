plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.java.library)
}

dependencies {
    // Project Modules
    api(project(":core:common"))
    api(project(":core:security"))
    api(project(":core:audit"))

    // DI
    ksp(libs.dagger.compiler)
    api(libs.dagger)
    api(libs.javax.inject) // Transitive for dagger

    // Testing
    testRuntimeOnly(libs.kotlin.test.junit5)
}

tasks.test {
    useJUnitPlatform()
}