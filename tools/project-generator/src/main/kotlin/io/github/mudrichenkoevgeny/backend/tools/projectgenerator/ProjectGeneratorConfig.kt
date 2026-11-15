package io.github.mudrichenkoevgeny.backend.tools.projectgenerator

import java.io.File

internal data class ProjectGeneratorConfig(
    val projectName: String,
    val projectTitle: String,
    val groupId: String,
    val packageName: String,
    val targetDir: File,
    val sdkRootDir: File,
    val linkLocal: Boolean,
    val skipGit: Boolean,
    val versionProperties: Map<String, String>
) {
    val packageDirPath: String = packageName.replace('.', '/')

    companion object {
        private const val PROP_SDK_ROOT_DIR = "sdkRootDir"
        private const val PROP_PROJECT_NAME = "projectName"
        private const val PROP_GROUP_ID = "groupId"
        private const val PROP_TARGET_DIR = "targetDir"
        private const val PROP_LINK_LOCAL = "linkLocal"
        private const val PROP_SKIP_GIT = "skipGit"

        private const val DEFAULT_GROUP_PREFIX = "com."
        private const val BACKEND_PACKAGE_SUFFIX = ".backend"
        private const val DEFAULT_BACKEND_SUFFIX = "-backend"

        private val KNOWN_VERSION_KEYS = listOf(
            "backendSdkVersion",
            "sharedFoundationVersion",
            "toolchainJvm",
            "kotlinVersion",
            "kspVersion",
            "detektVersion",
            "ktlintVersion",
            "coroutinesVersion",
            "serializationVersion",
            "ktorVersion",
            "daggerVersion",
            "javaxInjectVersion",
            "exposedVersion",
            "postgresqlVersion",
            "flywayVersion",
            "slf4jVersion",
            "junitVersion",
            "mockkVersion",
            "testcontainersVersion"
        )

        @Suppress("NoDirectSystemEnv")
        fun fromSystemProperties(): ProjectGeneratorConfig {
            val sdkRootDirPath = requireNotNull(System.getProperty(PROP_SDK_ROOT_DIR)) {
                "System property '$PROP_SDK_ROOT_DIR' is required"
            }
            val sdkRootDir = File(sdkRootDirPath).canonicalFile

            val rawName = System.getProperty(PROP_PROJECT_NAME)?.takeIf { it.isNotBlank() }
                ?: error("System property '$PROP_PROJECT_NAME' (or CLI -Pname) is required.")

            val sanitizedName = sanitizeName(rawName)
            val projectTitle = toPascalCase(sanitizedName)

            val rawGroup = System.getProperty(PROP_GROUP_ID)?.takeIf { it.isNotBlank() }
            val groupId = rawGroup ?: "$DEFAULT_GROUP_PREFIX${sanitizedName.lowercase().replace("-", "")}"

            val packageName = formatPackageName(groupId)

            val rawTargetDir = System.getProperty(PROP_TARGET_DIR)?.takeIf { it.isNotBlank() }
            val targetDir = if (rawTargetDir != null) {
                val f = File(rawTargetDir)
                if (f.isAbsolute) f else File(sdkRootDir, rawTargetDir).canonicalFile
            } else {
                File(sdkRootDir.parentFile, "$sanitizedName$DEFAULT_BACKEND_SUFFIX").canonicalFile
            }

            val linkLocal = System.getProperty(PROP_LINK_LOCAL)?.toBoolean() ?: false
            val skipGit = System.getProperty(PROP_SKIP_GIT)?.toBoolean() ?: false

            val versionProperties = KNOWN_VERSION_KEYS.associateWith { key ->
                val value = System.getProperty(key)
                check(!value.isNullOrBlank()) { "System property '$key' is missing or blank." }
                value
            }

            return ProjectGeneratorConfig(
                projectName = sanitizedName,
                projectTitle = projectTitle,
                groupId = groupId,
                packageName = packageName,
                targetDir = targetDir,
                sdkRootDir = sdkRootDir,
                linkLocal = linkLocal,
                skipGit = skipGit,
                versionProperties = versionProperties
            )
        }

        fun sanitizeName(name: String): String =
            name.lowercase()
                .replace(Regex("[^a-z0-9\\-]"), "-")
                .replace(Regex("-+"), "-")
                .trim('-')

        fun toPascalCase(hyphenated: String): String =
            hyphenated.split("-")
                .filter { it.isNotBlank() }
                .joinToString("") { part ->
                    part.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                }

        fun formatPackageName(groupId: String): String {
            val sanitizedGroup = groupId.split('.')
                .joinToString(".") { part -> part.replace(Regex("[^a-zA-Z0-9_]"), "").lowercase() }
            return if (sanitizedGroup.endsWith(BACKEND_PACKAGE_SUFFIX)) {
                sanitizedGroup
            } else {
                "$sanitizedGroup$BACKEND_PACKAGE_SUFFIX"
            }
        }
    }
}
