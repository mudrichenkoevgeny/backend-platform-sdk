package io.github.mudrichenkoevgeny.backend.tools.gradleplugin

import io.github.mudrichenkoevgeny.shared.foundation.tools.gradleplugin.utils.QualityUtils
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Gradle plugin configuring Ktlint and Detekt with shared base and backend-specific rule sets.
 */
public class BackendQualityPlugin : Plugin<Project> {
    /**
     * Applies shared and backend quality configurations to the target Gradle project.
     *
     * @param target The target Gradle project to configure.
     */
    override fun apply(target: Project) {
        target.pluginManager.apply(SHARED_FOUNDATION_QUALITY_PLUGIN_ID)

        QualityUtils.registerDetektConfigExtraction(
            target = target,
            taskName = DETEKT_TASK_NAME,
            classLoader = javaClass.classLoader,
            resourcePath = DETEKT_RESOURCE_PATH,
            outputFileName = DETEKT_OUTPUT_FILE_NAME
        )

        QualityUtils.registerEditorConfigLayer(
            target = target,
            classLoader = javaClass.classLoader,
            resourcePath = EDITORCONFIG_RESOURCE_PATH
        )

        val pluginVersion = QualityUtils.resolvePluginVersion(
            classLoader = javaClass.classLoader,
            versionResourceName = VERSION_RESOURCE_NAME,
            manifestPackage = javaClass.`package`
        )

        QualityUtils.applyDetektRulesDependency(
            target = target,
            expectedRootProjectName = SDK_ROOT_PROJECT_NAME,
            rulesProjectPath = DETEKT_RULES_PROJECT_PATH,
            mavenCoordinates = DETEKT_RULES_MAVEN_COORDINATES,
            pluginVersion = pluginVersion
        )
    }

    private companion object {
        private const val SHARED_FOUNDATION_QUALITY_PLUGIN_ID: String =
            "io.github.mudrichenkoevgeny.shared-foundation-quality"
        private const val DETEKT_TASK_NAME: String = "extractDetektBackendConfig"
        private const val DETEKT_RESOURCE_PATH: String = "config/detekt/detekt-backend.yml"
        private const val DETEKT_OUTPUT_FILE_NAME: String = "detekt-backend.yml"
        private const val EDITORCONFIG_RESOURCE_PATH: String = "config/editorconfig/editorconfig-backend.txt"
        private const val VERSION_RESOURCE_NAME: String = "backend-platform-sdk-plugin-version.txt"
        private const val SDK_ROOT_PROJECT_NAME: String = "backend-platform-sdk"
        private const val DETEKT_RULES_PROJECT_PATH: String = ":tools:detekt-rules"
        private const val DETEKT_RULES_MAVEN_COORDINATES: String =
            "io.github.mudrichenkoevgeny:backend-platform-sdk-detekt-rules"
    }
}
