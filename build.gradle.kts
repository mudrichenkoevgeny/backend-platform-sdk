import com.vanniktech.maven.publish.JavaPlatform
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import dev.detekt.gradle.Detekt
import dev.detekt.gradle.extensions.DetektExtension
import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import java.util.Properties

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
    group = "io.github.mudrichenkoevgeny"
    version = "0.0.20"
}

val daemonJvmVersion: Int = run {
    val daemonFile = rootProject.file("gradle/gradle-daemon-jvm.properties")
    if (daemonFile.exists()) {
        val properties = Properties().apply {
            daemonFile.inputStream().use { load(it) }
        }
        properties.getProperty("toolchainVersion")?.toIntOrNull()
    } else {
        null
    }
} ?: libs.versions.jvmToolchain.get().toInt()

subprojects {
    val isBom = project.name == "bom"
    val isTool = project.path.startsWith(":tools")
    val isModule = project.projectDir.resolve("src").exists() || isBom

    if (!isModule) {
        return@subprojects
    }

    pluginManager.apply("com.autonomousapps.dependency-analysis")

    configureKtlint(isTool)

    if (!isBom) {
        configureDetekt()
    }

    configureKotlinCompiler(
        daemonJvmVersion,
        isBom
    )

    afterEvaluate {
        val explicitPublishFlag = (project.findProperty("publishToMaven") as? Boolean) ?: project.findProperty(
            "publishToMaven"
        )?.toString()?.toBoolean()
        val shouldPublishToMaven = (!isTool) || (explicitPublishFlag == true)

        if (shouldPublishToMaven) {
            configureMavenPublishingAndDokka(isBom)
        }
    }
}

private fun Project.configureDetekt() {
    pluginManager.apply("dev.detekt")

    extensions.configure<DetektExtension> {
        buildUponDefaultConfig = true
        autoCorrect = true
        allRules = false
        config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
        source.setFrom(files("src"))
    }

    if (path != ":tools:detekt-rules") {
        dependencies {
            add(
                "detektPlugins",
                project(":tools:detekt-rules")
            )
        }

        tasks.withType<Detekt>().configureEach {
            dependsOn(":tools:detekt-rules:jar")
            pluginClasspath.setFrom(rootProject.project(":tools:detekt-rules").tasks.named("jar"))
        }
    } else {
        tasks.withType<Detekt>().configureEach {
            dependsOn("jar")
            pluginClasspath.setFrom(tasks.named("jar"))
        }
    }
}

private fun Project.configureKtlint(isTool: Boolean) {
    pluginManager.apply("org.jlleitschuh.gradle.ktlint")

    extensions.configure<KtlintExtension> {
        ignoreFailures.set(isTool)
        filter {
            exclude {
                val path = it.file.absolutePath
                path.contains("build") || path.contains("generated")
            }
        }
    }

    tasks.matching { it.name.startsWith("ktlint") }.configureEach {
        if (this is SourceTask) {
            exclude { it.file.absolutePath.contains("build") || it.file.absolutePath.contains("generated") }
        }
    }
}

private fun Project.configureKotlinCompiler(
    daemonJvmVersion: Int,
    isBom: Boolean
) {
    if (isBom) {
        pluginManager.apply("java-platform")
        return
    }

    pluginManager.apply("org.jetbrains.kotlin.jvm")

    val targetJvmVersionString = rootProject.libs.versions.targetJvm.get()
    val targetJvm = JvmTarget.fromTarget(targetJvmVersionString)

    extensions.configure<KotlinJvmProjectExtension>("kotlin") {
        explicitApi()
        jvmToolchain(daemonJvmVersion)
        compilerOptions {
            jvmTarget.set(targetJvm)
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.release.set(targetJvmVersionString.toInt())
    }

    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            allWarningsAsErrors.set(true)

            freeCompilerArgs.add("-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi")
            freeCompilerArgs.add("-opt-in=kotlin.concurrent.atomics.ExperimentalAtomicApi")
        }
    }

    tasks.withType<Test>().configureEach {
        jvmArgs("-XX:+EnableDynamicAgentLoading")
    }
}

private fun Project.configureMavenPublishingAndDokka(isBom: Boolean) {
    pluginManager.apply("com.vanniktech.maven.publish")
    pluginManager.apply("org.jetbrains.dokka")

    rootProject.dependencies {
        add(
            "dokka",
            project(this@configureMavenPublishingAndDokka.path)
        )
    }

    extensions.configure<DokkaExtension> {
        dokkaPublications.configureEach {
            failOnWarning.set(true)
        }
    }

    extensions.configure<MavenPublishBaseExtension> {
        val customArtifactId = project.findProperty("artifactId") as? String
        val projectPathName = project.path.removePrefix(":").replace(
            ":",
            "-"
        ).replace(
            ".",
            "-"
        )

        val newArtifactId = customArtifactId ?: if (projectPathName.startsWith(rootProject.name)) {
            projectPathName
        } else {
            "${rootProject.name}-$projectPathName"
        }

        coordinates(
            project.group.toString(),
            newArtifactId,
            project.version.toString()
        )

        publishToMavenCentral()
        signAllPublications()

        if (isBom) {
            configure(JavaPlatform())
        } else {
            configure(KotlinJvm(javadocJar = JavadocJar.Javadoc()))
        }

        pom {
            name.set("Backend SDK - ${project.name}")
            description.set("Module ${project.name} of the backend-platform-sdk")
            url.set("https://github.com/mudrichenkoevgeny/backend-platform-sdk")
            licenses {
                license {
                    name.set("The Apache License, Version 2.0")
                    url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                }
            }
            developers {
                developer {
                    id.set("mudrichenkoevgeny")
                    name.set("Evgeny Mudrichenko")
                    email.set("evgeny.mudrichenko@gmail.com")
                }
            }
            scm {
                connection.set("scm:git:git://github.com/mudrichenkoevgeny/backend-platform-sdk.git")
                developerConnection.set("scm:git:ssh://github.com/mudrichenkoevgeny/backend-platform-sdk.git")
                url.set("https://github.com/mudrichenkoevgeny/backend-platform-sdk")
            }
        }
    }
}
