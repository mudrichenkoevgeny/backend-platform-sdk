package io.github.mudrichenkoevgeny.backend.tools.gradleplugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/**
 * Gradle plugin configuring Kotlin compiler options and opt-ins for backend modules.
 */
public class BackendKotlinConventionPlugin : Plugin<Project> {
    /**
     * Applies shared Kotlin conventions and backend-specific compiler opt-ins to the target project.
     *
     * @param target The target Gradle project to configure.
     */
    override fun apply(target: Project) {
        target.pluginManager.apply(SHARED_FOUNDATION_KOTLIN_PLUGIN_ID)

        if (!isBackendPlatformSdk(target)) {
            target.plugins.withId(PLUGIN_KOTLIN_JVM) {
                target.extensions.configure(KotlinJvmProjectExtension::class.java) { kotlin ->
                    kotlin.explicitApi = ExplicitApiMode.Disabled
                }
            }
        }

        target.tasks.withType(Test::class.java).configureEach { testTask ->
            testTask.useJUnitPlatform()
        }
    }

    private fun isBackendPlatformSdk(target: Project): Boolean =
        target.rootProject.name == BACKEND_SDK_ROOT_PROJECT_NAME ||
            target.group.toString().startsWith(BACKEND_SDK_GROUP_PREFIX)

    private companion object {
        private const val SHARED_FOUNDATION_KOTLIN_PLUGIN_ID: String =
            "io.github.mudrichenkoevgeny.shared-foundation-kotlin"
        private const val PLUGIN_KOTLIN_JVM: String =
            "org.jetbrains.kotlin.jvm"
        private const val BACKEND_SDK_ROOT_PROJECT_NAME: String =
            "backend-platform-sdk"
        private const val BACKEND_SDK_GROUP_PREFIX: String =
            "io.github.mudrichenkoevgeny.backend"
    }
}
