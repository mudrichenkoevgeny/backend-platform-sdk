package io.github.mudrichenkoevgeny.backend.core.common.config.pathresolver

import io.github.mudrichenkoevgeny.backend.core.common.config.common.envkeys.CommonEnvKeys
import java.io.File

/**
 * Configuration used by [PathResolver] to locate env and secrets resources.
 *
 * Defaults rely on the current working directory and process environment variables.
 *
 * @param projectRoot root directory of the project, used as a base for relative paths.
 * @param envFilePath optional path to the env file, read from [CommonEnvKeys.ENV_FILE].
 * @param secretsDirPath optional path to the secrets directory, read from [CommonEnvKeys.SECRETS_DIR].
 */
data class PathResolverConfig(
    val projectRoot: File = File(".").canonicalFile,
    val envFilePath: String? = System.getenv(CommonEnvKeys.ENV_FILE),
    val secretsDirPath: String? = System.getenv(CommonEnvKeys.SECRETS_DIR)
)