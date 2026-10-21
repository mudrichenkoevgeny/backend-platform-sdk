import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties
import kotlin.apply

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.maven.publish) apply false
    alias(libs.plugins.dependency.analysis)
    alias(libs.plugins.gradle.versions.plugin)
}

allprojects {
    group = "io.github.mudrichenkoevgeny"
    version = "0.0.20"
}

val daemonJvmVersion: String = run {
    val daemonFile = rootProject.file("gradle/gradle-daemon-jvm.properties")
    if (daemonFile.exists()) {
        val properties = Properties().apply {
            daemonFile.inputStream().use { load(it) }
        }
        properties.getProperty("toolchainVersion")
    } else {
        null
    }
} ?: libs.versions.jvmToolchain.get()

subprojects {
    val isBom = project.name == "bom"
    val isModule = project.projectDir.resolve("src").exists() || isBom

    if (!isModule) {
        return@subprojects
    }

    pluginManager.apply("com.vanniktech.maven.publish")
    pluginManager.apply("com.autonomousapps.dependency-analysis")

    if (isBom) {
        pluginManager.apply("java-platform")
    } else {
        pluginManager.apply("org.jetbrains.kotlin.jvm")

        tasks.withType<KotlinCompile>().configureEach {
            compilerOptions {
                jvmTarget.set(JvmTarget.fromTarget(daemonJvmVersion))

                freeCompilerArgs.add("-opt-in=kotlin.uuid.ExperimentalUuidApi")
                freeCompilerArgs.add("-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi")
                freeCompilerArgs.add("-opt-in=kotlin.concurrent.atomics.ExperimentalAtomicApi")
            }
        }

        tasks.withType<Test>().configureEach {
            jvmArgs("-XX:+EnableDynamicAgentLoading")
        }
    }

    extensions.configure<MavenPublishBaseExtension> {
        val projectPathName = project.path
            .removePrefix(":")
            .replace(":", "-")
            .replace(".", "-")

        val newArtifactId = if (projectPathName.startsWith(rootProject.name)) {
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
            configure(com.vanniktech.maven.publish.JavaPlatform())
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