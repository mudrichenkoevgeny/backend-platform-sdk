package io.github.mudrichenkoevgeny.backend.tools.gradleplugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test

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
        target.pluginManager.apply("io.github.mudrichenkoevgeny.shared-foundation-kotlin")

        target.tasks.withType(Test::class.java).configureEach { testTask ->
            testTask.useJUnitPlatform()
        }
    }
}
