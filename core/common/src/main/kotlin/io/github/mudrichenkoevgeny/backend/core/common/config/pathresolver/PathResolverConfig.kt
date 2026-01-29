package io.github.mudrichenkoevgeny.backend.core.common.config.pathresolver

import io.github.mudrichenkoevgeny.backend.core.common.config.common.envkeys.CommonEnvKeys
import java.io.File

data class PathResolverConfig(
    val projectRoot: File = File(".").canonicalFile,
    val envFilePath: String? = System.getenv(CommonEnvKeys.ENV_FILE),
    val secretsDirPath: String? = System.getenv(CommonEnvKeys.SECRETS_DIR)
)