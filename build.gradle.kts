import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.maven.publish) apply false
}

allprojects {
    group = "io.github.mudrichenkoevgeny"
    version = "0.0.3"

    repositories {
        mavenCentral()
    }
}

subprojects {
    val isModule = file("src").exists()
    if (!isModule) return@subprojects

    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "com.vanniktech.maven.publish")

    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget(libs.versions.jvmToolchain.get()))
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

        configure(KotlinJvm(javadocJar = JavadocJar.Javadoc()))

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