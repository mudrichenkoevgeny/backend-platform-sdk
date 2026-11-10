plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.maven.publish) apply false
    alias(libs.plugins.dependency.analysis)
    alias(libs.plugins.gradle.versions.plugin)
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.binary.compatibility.validator)
    alias(libs.plugins.dokka)
    alias(libs.plugins.shared.foundation.agent.sync)
    alias(libs.plugins.shared.foundation.quality) apply false
    alias(libs.plugins.shared.foundation.kotlin) apply false
    alias(libs.plugins.shared.foundation.publish) apply false
}

apiValidation {
    // Disabled to allow fast API iteration without breaking builds on signature changes.
    // Re-enable if strict binary backward compatibility (ABI) checks are required.
    validationDisabled = true
    ignoredProjects += listOf(
        "bom",
        "sample",
        "detekt-rules"
    )
}

allprojects {
    group = rootProject.group
    version = rootProject.version
}

subprojects {
    val isBom = project.name == "bom"
    if (!isBom) {
        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
            compilerOptions {
                optIn.add("kotlinx.coroutines.ExperimentalCoroutinesApi")
                optIn.add("kotlin.concurrent.atomics.ExperimentalAtomicApi")
            }
        }
    }
}


