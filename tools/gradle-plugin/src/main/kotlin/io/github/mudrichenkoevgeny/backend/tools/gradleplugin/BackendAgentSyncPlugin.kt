package io.github.mudrichenkoevgeny.backend.tools.gradleplugin

import io.github.mudrichenkoevgeny.shared.foundation.tools.gradleplugin.utils.AgentSyncUtils
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Gradle plugin for synchronizing shared and backend AI agent rules into consumer projects.
 */
public class BackendAgentSyncPlugin : Plugin<Project> {
    /**
     * Applies the shared and backend agent sync task configurations to the target Gradle project.
     *
     * @param target The target Gradle project to configure.
     */
    override fun apply(target: Project) {
        if (target != target.rootProject) {
            target.logger.warn(WARN_ROOT_PROJECT_ONLY)
            return
        }

        target.pluginManager.apply(SHARED_FOUNDATION_AGENT_SYNC_PLUGIN_ID)

        @Suppress("PreferValueOverName")
        val isRootSdkProject = target.rootProject.name == SDK_ROOT_PROJECT_NAME

        if (!isRootSdkProject) {
            AgentSyncUtils.registerAgentRulesLayer(
                target = target,
                classLoader = javaClass.classLoader,
                resourcePath = AGENT_RULES_RESOURCE_PATH,
                targetSubdir = TARGET_SUBDIR
            )
        }
    }

    private companion object {
        private const val WARN_ROOT_PROJECT_ONLY: String =
            "BackendAgentSyncPlugin should only be applied to the root project"
        private const val SHARED_FOUNDATION_AGENT_SYNC_PLUGIN_ID: String =
            "io.github.mudrichenkoevgeny.shared-foundation-agent-sync"
        private const val SDK_ROOT_PROJECT_NAME: String = "backend-platform-sdk"
        private const val AGENT_RULES_RESOURCE_PATH: String = "agent-rules/backend"
        private const val TARGET_SUBDIR: String = "backend"
    }
}
