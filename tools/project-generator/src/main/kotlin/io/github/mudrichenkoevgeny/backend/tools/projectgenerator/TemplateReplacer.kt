package io.github.mudrichenkoevgeny.backend.tools.projectgenerator

internal class TemplateReplacer(
    private val config: ProjectGeneratorConfig
) {
    fun replaceTokens(templateText: String): String {
        val substitutions = buildCoreSubstitutions() + buildVersionSubstitutions()
        var result = templateText
        for ((token, value) in substitutions) {
            result = result.replace(token, value)
        }
        return result
    }

    private fun buildCoreSubstitutions(): Map<String, String> {
        val mavenLocalRepos = if (config.linkLocal) MAVEN_LOCAL_REPOS_SNIPPET else ""

        return mapOf(
            TOKEN_PROJECT_NAME to config.projectName,
            TOKEN_PROJECT_TITLE to config.projectTitle,
            TOKEN_PROJECT_REPO_NAME to "${config.projectName}$DEFAULT_BACKEND_SUFFIX",
            TOKEN_GROUP_ID to config.groupId,
            TOKEN_PACKAGE_NAME to config.packageName,
            TOKEN_PACKAGE_DIR_PATH to config.packageDirPath,
            TOKEN_MAVEN_LOCAL_REPOS to mavenLocalRepos
        )
    }

    private fun buildVersionSubstitutions(): Map<String, String> {
        val props = config.versionProperties
        return mapOf(
            TOKEN_BACKEND_SDK_VERSION to (props["backendSdkVersion"] ?: ""),
            TOKEN_SF_VERSION to (props["sharedFoundationVersion"] ?: ""),
            TOKEN_TOOLCHAIN_JVM to (props["toolchainJvm"] ?: "25"),
            TOKEN_KOTLIN_VERSION to (props["kotlinVersion"] ?: ""),
            TOKEN_KSP_VERSION to (props["kspVersion"] ?: ""),
            TOKEN_DETEKT_VERSION to (props["detektVersion"] ?: ""),
            TOKEN_KTLINT_VERSION to (props["ktlintVersion"] ?: ""),
            TOKEN_COROUTINES_VERSION to (props["coroutinesVersion"] ?: ""),
            TOKEN_SERIALIZATION_VERSION to (props["serializationVersion"] ?: ""),
            TOKEN_KTOR_VERSION to (props["ktorVersion"] ?: ""),
            TOKEN_DAGGER_VERSION to (props["daggerVersion"] ?: ""),
            TOKEN_JAVAX_INJECT_VERSION to (props["javaxInjectVersion"] ?: ""),
            TOKEN_EXPOSED_VERSION to (props["exposedVersion"] ?: ""),
            TOKEN_POSTGRESQL_VERSION to (props["postgresqlVersion"] ?: ""),
            TOKEN_FLYWAY_VERSION to (props["flywayVersion"] ?: ""),
            TOKEN_SLF4J_VERSION to (props["slf4jVersion"] ?: ""),
            TOKEN_JUNIT_VERSION to (props["junitVersion"] ?: ""),
            TOKEN_MOCKK_VERSION to (props["mockkVersion"] ?: ""),
            TOKEN_TESTCONTAINERS_VERSION to (props["testcontainersVersion"] ?: "")
        )
    }

    private companion object {
        private const val MAVEN_LOCAL_REPOS_SNIPPET = "        mavenLocal()\n"
        private const val DEFAULT_BACKEND_SUFFIX = "-backend"

        private const val TOKEN_PROJECT_NAME = "{{PROJECT_NAME}}"
        private const val TOKEN_PROJECT_TITLE = "{{PROJECT_TITLE}}"
        private const val TOKEN_PROJECT_REPO_NAME = "{{PROJECT_REPO_NAME}}"
        private const val TOKEN_GROUP_ID = "{{GROUP_ID}}"
        private const val TOKEN_PACKAGE_NAME = "{{PACKAGE_NAME}}"
        private const val TOKEN_PACKAGE_DIR_PATH = "{{PACKAGE_DIR_PATH}}"
        private const val TOKEN_MAVEN_LOCAL_REPOS = "{{MAVEN_LOCAL_REPOS}}"

        private const val TOKEN_BACKEND_SDK_VERSION = "{{BACKEND_SDK_VERSION}}"
        private const val TOKEN_SF_VERSION = "{{SF_VERSION}}"
        private const val TOKEN_TOOLCHAIN_JVM = "{{TOOLCHAIN_JVM}}"
        private const val TOKEN_KOTLIN_VERSION = "{{KOTLIN_VERSION}}"
        private const val TOKEN_KSP_VERSION = "{{KSP_VERSION}}"
        private const val TOKEN_DETEKT_VERSION = "{{DETEKT_VERSION}}"
        private const val TOKEN_KTLINT_VERSION = "{{KTLINT_VERSION}}"
        private const val TOKEN_COROUTINES_VERSION = "{{COROUTINES_VERSION}}"
        private const val TOKEN_SERIALIZATION_VERSION = "{{SERIALIZATION_VERSION}}"
        private const val TOKEN_KTOR_VERSION = "{{KTOR_VERSION}}"
        private const val TOKEN_DAGGER_VERSION = "{{DAGGER_VERSION}}"
        private const val TOKEN_JAVAX_INJECT_VERSION = "{{JAVAX_INJECT_VERSION}}"
        private const val TOKEN_EXPOSED_VERSION = "{{EXPOSED_VERSION}}"
        private const val TOKEN_POSTGRESQL_VERSION = "{{POSTGRESQL_VERSION}}"
        private const val TOKEN_FLYWAY_VERSION = "{{FLYWAY_VERSION}}"
        private const val TOKEN_SLF4J_VERSION = "{{SLF4J_VERSION}}"
        private const val TOKEN_JUNIT_VERSION = "{{JUNIT_VERSION}}"
        private const val TOKEN_MOCKK_VERSION = "{{MOCKK_VERSION}}"
        private const val TOKEN_TESTCONTAINERS_VERSION = "{{TESTCONTAINERS_VERSION}}"
    }
}
