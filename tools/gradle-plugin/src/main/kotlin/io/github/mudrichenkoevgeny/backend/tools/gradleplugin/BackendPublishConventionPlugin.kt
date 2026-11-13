package io.github.mudrichenkoevgeny.backend.tools.gradleplugin

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Gradle plugin configuring Maven publishing conventions for backend modules.
 */
public class BackendPublishConventionPlugin : Plugin<Project> {
    /**
     * Applies shared Maven publishing conventions to the target Gradle project.
     *
     * @param target The target Gradle project to configure.
     */
    override fun apply(target: Project) {
        target.pluginManager.apply("io.github.mudrichenkoevgeny.shared-foundation-publish")
    }
}
