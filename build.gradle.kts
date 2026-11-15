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
    alias(libs.plugins.backend.platform.agent.sync)
}

apiValidation {
    validationDisabled = true
    ignoredProjects += listOf(
        "bom",
        "sample",
        "detekt-rules",
        "project-generator"
    )
}

allprojects {
    group = rootProject.group
    version = rootProject.version
}

tasks.register("initProject") {
    group = "scaffolding"
    description = "Scaffolds a new backend microservice project"
    dependsOn(":tools:project-generator:initProject")
}

tasks.register("scaffoldProject") {
    group = "scaffolding"
    description = "Alias for initProject"
    dependsOn(":tools:project-generator:scaffoldProject")
}

tasks.register("verifyProjectGenerator") {
    group = "verification"
    description = "Scaffolds a test project and runs ./gradlew check jar E2E"
    dependsOn(":tools:project-generator:verifyProjectGenerator")
}
