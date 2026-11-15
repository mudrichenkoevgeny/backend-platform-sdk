plugins {
    alias(libs.plugins.backend.platform.quality)
    alias(libs.plugins.backend.platform.kotlin)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.java.library)
}

dependencies {
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.kotlin.test.junit5)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

fun JavaExec.configureVersionProperties() {
    systemProperty("sdkRootDir", rootProject.rootDir.absolutePath)
    systemProperty("backendSdkVersion", rootProject.version.toString())
    systemProperty("sharedFoundationVersion", rootProject.libs.versions.shared.foundation.get())
    systemProperty("toolchainJvm", rootProject.libs.versions.jvmToolchain.get())
    systemProperty("kotlinVersion", rootProject.libs.versions.kotlin.asProvider().get())
    systemProperty("kspVersion", rootProject.libs.versions.ksp.get())
    systemProperty("detektVersion", rootProject.libs.versions.detekt.get())
    systemProperty("ktlintVersion", rootProject.libs.versions.ktlint.get())
    systemProperty("coroutinesVersion", rootProject.libs.versions.kotlinx.coroutines.get())
    systemProperty("serializationVersion", rootProject.libs.versions.kotlinx.serialization.get())
    systemProperty("ktorVersion", rootProject.libs.versions.ktor.get())
    systemProperty("daggerVersion", rootProject.libs.versions.dagger.get())
    systemProperty("javaxInjectVersion", rootProject.libs.versions.javax.inject.get())
    systemProperty("exposedVersion", rootProject.libs.versions.exposed.get())
    systemProperty("postgresqlVersion", rootProject.libs.versions.postgresql.get())
    systemProperty("flywayVersion", rootProject.libs.versions.flyway.get())
    systemProperty("slf4jVersion", rootProject.libs.versions.slf4j.get())
    systemProperty("junitVersion", rootProject.libs.versions.junit.get())
    systemProperty("mockkVersion", rootProject.libs.versions.mockk.get())
    systemProperty("testcontainersVersion", rootProject.libs.versions.testcontainers.get())
}

val initProject = tasks.register("initProject", JavaExec::class) {
    group = "scaffolding"
    description = "Scaffolds a new backend microservice project"
    mainClass.set("io.github.mudrichenkoevgeny.backend.tools.projectgenerator.ProjectGeneratorKt")
    classpath = sourceSets.main.get().runtimeClasspath
    configureVersionProperties()

    val customName = project.providers.gradleProperty("name").orNull
    val customGroup = project.providers.gradleProperty("groupId").orNull
        ?: project.providers.gradleProperty("group").orNull?.takeIf { it != rootProject.group.toString() }
        ?: ""
    val customTargetDir = project.providers.gradleProperty("targetDir").orNull
    val customLinkLocal = project.providers.gradleProperty("linkLocal").orNull
    val customSkipGit = project.providers.gradleProperty("skipGit").orNull

    customName?.let { systemProperty("projectName", it) }
    if (customGroup.isNotEmpty()) systemProperty("groupId", customGroup)
    customTargetDir?.let { systemProperty("targetDir", it) }
    customLinkLocal?.let { systemProperty("linkLocal", it) }
    customSkipGit?.let { systemProperty("skipGit", it) }
}

tasks.register("scaffoldProject") {
    group = "scaffolding"
    description = "Alias for initProject"
    dependsOn(initProject)
}

tasks.register("verifyProjectGenerator", JavaExec::class) {
    group = "verification"
    description = "Scaffolds a test project and runs ./gradlew check jar E2E"
    mainClass.set("io.github.mudrichenkoevgeny.backend.tools.projectgenerator.ProjectGeneratorVerifierKt")
    classpath = sourceSets.main.get().runtimeClasspath
    configureVersionProperties()

    dependsOn(rootProject.getTasksByName("publishToMavenLocal", true))

    val pluginBuild = gradle.includedBuilds.find { it.name.endsWith("gradle-plugin") }
    if (pluginBuild != null) {
        dependsOn(pluginBuild.task(":publishToMavenLocal"))
    }
}
