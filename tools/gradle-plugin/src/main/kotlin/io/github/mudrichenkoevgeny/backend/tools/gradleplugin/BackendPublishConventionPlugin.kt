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
        target.pluginManager.apply(SHARED_FOUNDATION_PUBLISH_PLUGIN_ID)

        target.pluginManager.withPlugin(JAVA_TEST_FIXTURES_PLUGIN_ID) {
            val parentProject = target.parent
            val rootProject = target.rootProject
            val moduleGroup = parentProject?.name
            val sdkArtifactId =
                if (!moduleGroup.isNullOrBlank() && moduleGroup != rootProject.name) {
                    "backend-platform-sdk-$moduleGroup-${target.name}"
                } else {
                    "backend-platform-sdk-${target.name}"
                }
            target.configurations
                .matching { it.name.startsWith(TEST_FIXTURES_CONFIG_PREFIX) }
                .configureEach { config ->
                    config.outgoing.capability("${target.group}:$sdkArtifactId-test-fixtures:${target.version}")
                }
        }
    }

    private companion object {
        private const val SHARED_FOUNDATION_PUBLISH_PLUGIN_ID: String =
            "io.github.mudrichenkoevgeny.shared-foundation-publish"
        private const val JAVA_TEST_FIXTURES_PLUGIN_ID: String = "java-test-fixtures"
        private const val TEST_FIXTURES_CONFIG_PREFIX: String = "testFixtures"
    }
}
