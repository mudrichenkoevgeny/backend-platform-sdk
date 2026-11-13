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
            target.logger.warn("BackendAgentSyncPlugin should only be applied to the root project")
            return
        }

        target.pluginManager.apply("io.github.mudrichenkoevgeny.shared-foundation-agent-sync")

        if (target.rootProject.name != "backend-platform-sdk") {
            AgentSyncUtils.registerAgentRulesLayer(
                target = target,
                classLoader = javaClass.classLoader,
                resourcePath = "agent-rules/backend",
                targetSubdir = "backend"
            )
        }
    }
}
