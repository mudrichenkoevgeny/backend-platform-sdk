import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.maven.publish) apply false
    alias(libs.plugins.dependency.analysis)
}

allprojects {
    group = "io.github.mudrichenkoevgeny"
    version = "0.0.4"
}

subprojects {
    val isBom = project.name == "bom"
    val isModule = file("src").exists() || isBom
    if (!isModule) {
        return@subprojects
    }

    apply(plugin = "com.vanniktech.maven.publish")
    apply(plugin = "com.autonomousapps.dependency-analysis")

    if (isBom) {
        apply(plugin = "java-platform")
    } else {
        apply(plugin = "org.jetbrains.kotlin.jvm")

        tasks.withType<KotlinCompile>().configureEach {
            compilerOptions {
                jvmTarget.set(JvmTarget.fromTarget(libs.versions.jvmToolchain.get()))
            }
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

    configure<PublishingExtension> {
        publications.withType<MavenPublication> {
            pom.withXml {
                val dependenciesNode = asNode().get("dependencies") as? groovy.util.Node ?: return@withXml
                val runtimeConfig = configurations.findByName("runtimeClasspath") ?: return@withXml

                runtimeConfig.resolvedConfiguration.resolvedArtifacts.forEach { artifact ->
                    val dep = (dependenciesNode.children().find {
                        val node = it as groovy.util.Node
                        node.get("groupId") == artifact.moduleVersion.id.group &&
                                node.get("artifactId") == artifact.moduleVersion.id.name
                    } as? groovy.util.Node)

                    if (dep != null && (dep.get("version") == null || (dep.get("version") as groovy.util.NodeList).isEmpty())) {
                        dep.appendNode("version", artifact.moduleVersion.id.version)
                    }
                }
            }
        }
    }
}