val repoRootDir: File = rootDir.parentFile.parentFile

plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-gradle-plugin`
    alias(libs.plugins.shared.foundation.kotlin)
    alias(libs.plugins.shared.foundation.quality)
    alias(libs.plugins.shared.foundation.publish)
}

gradlePlugin {
    plugins {
        create("backendAgentSync") {
            id = "io.github.mudrichenkoevgeny.backend-platform-agent-sync"
            implementationClass = "io.github.mudrichenkoevgeny.backend.tools.gradleplugin.BackendAgentSyncPlugin"
            displayName = "Backend Platform Agent Sync Gradle Plugin"
            description = "Gradle plugin synchronizing backend platform AI agent rules into consumer projects"
        }
        create("backendQuality") {
            id = "io.github.mudrichenkoevgeny.backend-platform-quality"
            implementationClass = "io.github.mudrichenkoevgeny.backend.tools.gradleplugin.BackendQualityPlugin"
            displayName = "Backend Platform Quality Gradle Plugin"
            description = "Gradle plugin configuring Detekt with Ktor/Exposed rules and Ktlint"
        }
        create("backendKotlinConvention") {
            id = "io.github.mudrichenkoevgeny.backend-platform-kotlin"
            implementationClass = "io.github.mudrichenkoevgeny.backend.tools.gradleplugin.BackendKotlinConventionPlugin"
            displayName = "Backend Platform Kotlin Convention Gradle Plugin"
            description = "Gradle plugin configuring Kotlin JVM compiler options and explicitApi for backend services"
        }
        create("backendPublishConvention") {
            id = "io.github.mudrichenkoevgeny.backend-platform-publish"
            implementationClass =
                "io.github.mudrichenkoevgeny.backend.tools.gradleplugin.BackendPublishConventionPlugin"
            displayName = "Backend Platform Maven Publishing Convention Gradle Plugin"
            description =
                "Gradle plugin configuring Vanniktech Maven Publishing, Dokka, and POM metadata for backend modules"
        }
    }
}

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.detekt.gradle.plugin)
    implementation(libs.ktlint.gradle.plugin)
    implementation(libs.maven.publish.gradle.plugin)
    implementation(libs.dokka.gradle.plugin)
    implementation(libs.shared.foundation.gradle.plugin)
}

val generatePluginVersionResource = tasks.register("generatePluginVersionResource") {
    group = "build"
    description = "Generates plugin version resource file"
    val currentVersion = project.version.toString()
    inputs.property("version", currentVersion)
    val versionFile = layout.buildDirectory.file("generated/resources/backend-platform-sdk-plugin-version.txt")
    outputs.file(versionFile)
    doLast {
        val file = versionFile.get().asFile
        file.parentFile.mkdirs()
        file.writeText(currentVersion)
    }
}

tasks.named<ProcessResources>("processResources") {
    from(generatePluginVersionResource)
    from(repoRootDir.resolve(".agent/global/backend")) {
        into("agent-rules/backend")
    }
}
