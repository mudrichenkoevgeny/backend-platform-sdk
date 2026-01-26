import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    `maven-publish`
    signing
}

allprojects {
    group = "io.github.mudrichenko-evgeny"
    version = "0.0.2"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "kotlin")
    apply(plugin = "maven-publish")
    apply(plugin = "signing")

    configure<JavaPluginExtension> {
        withSourcesJar()
        withJavadocJar()
    }

    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget(libs.versions.jvmToolchain.get()))
        }
    }

    configure<PublishingExtension> {
        publications {
            create<MavenPublication>("maven") {
                from(components["kotlin"])
                artifactId = project.name

                pom {
                    name.set("Backend SDK - ${project.name}")
                    description.set("Module ${project.name} of the backend-platform-sdk")
                    url.set("https://github.com/mudrichenko-evgeny/backend-platform-sdk")
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
                        connection.set("scm:git:git://github.com/mudrichenko-evgeny/backend-platform-sdk.git")
                        developerConnection.set("scm:git:ssh://github.com/mudrichenko-evgeny/backend-platform-sdk.git")
                        url.set("https://github.com/mudrichenko-evgeny/backend-platform-sdk")
                    }
                }
            }
        }

        repositories {
            maven {
                name = "OSSRH"
                url = uri("https://central.sonatype.com/api/v1/publisher/upload")
                credentials {
                    username = project.property("mavenCentralUsername").toString()
                    password = project.property("mavenCentralPassword").toString()
                }
            }
        }
    }

    configure<SigningExtension> {
        val isSigningKeyPresent = project.hasProperty("signing.keyId")
        if (isSigningKeyPresent) {
            sign(extensions.getByType<PublishingExtension>().publications["maven"])
        }
    }
}