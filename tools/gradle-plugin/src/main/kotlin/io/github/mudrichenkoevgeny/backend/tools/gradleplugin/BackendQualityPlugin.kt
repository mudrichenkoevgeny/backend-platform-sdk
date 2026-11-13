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
        target.pluginManager.apply("io.github.mudrichenkoevgeny.shared-foundation-quality")

        QualityUtils.registerDetektConfigExtraction(
            target = target,
            taskName = "extractDetektBackendConfig",
            classLoader = javaClass.classLoader,
            resourcePath = "config/detekt/detekt-backend.yml",
            outputFileName = "detekt-backend.yml"
        )

        QualityUtils.registerEditorConfigLayer(
            target = target,
            classLoader = javaClass.classLoader,
            resourcePath = "config/editorconfig/editorconfig-backend.txt"
        )

        val pluginVersion = QualityUtils.resolvePluginVersion(
            classLoader = javaClass.classLoader,
            versionResourceName = "backend-platform-sdk-plugin-version.txt",
            manifestPackage = javaClass.`package`
        )

        QualityUtils.applyDetektRulesDependency(
            target = target,
            expectedRootProjectName = "backend-platform-sdk",
            rulesProjectPath = ":tools:detekt-rules",
            mavenCoordinates = "io.github.mudrichenkoevgeny:backend-platform-sdk-detekt-rules",
            pluginVersion = pluginVersion
        )
    }
}
