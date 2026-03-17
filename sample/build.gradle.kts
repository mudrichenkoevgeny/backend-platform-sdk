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
    api(project(":core:settings"))
    api(project(":core:security"))
    api(project(":core:audit"))
    api(project(":core:events"))
    api(project(":core:observability"))
    api(project(":core:storage"))
    api(project(":core:crosscutting"))
    api(project(":feature:user"))

    // Shared foundation
    implementation(libs.shared.foundation.core.security) // Transitive
    implementation(libs.slf4j) // Transitive

    // Kotlin
    implementation(libs.kotlinx.coroutines.core) // Transitive

    // Ktor
    api(libs.ktor.server.core)
    api(libs.ktor.events) // Transitive for Ktor

    // DI
    ksp(libs.dagger.compiler)
    api(libs.dagger)
    api(libs.javax.inject) // Transitive for dagger

    // Database
    implementation(libs.exposed.jdbc)

    // Testing
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.kotlin.test.junit5)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.mockk)
}

tasks.test {
    useJUnitPlatform()
}

tasks.register("copyLibs", Copy::class) {
    from(configurations.runtimeClasspath)
    into(layout.buildDirectory.dir("libs/lib"))
}

tasks.jar {
    archiveFileName.set("app.jar")
    finalizedBy("copyLibs")
    manifest {
        attributes["Main-Class"] = "io.github.mudrichenkoevgeny.backend.sample.MainKt"
        attributes["Class-Path"] = configurations.runtimeClasspath.get()
            .joinToString(" ") { "lib/${it.name}" }
    }
}