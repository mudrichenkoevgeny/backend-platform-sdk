package io.github.mudrichenkoevgeny.backend.core.common.config.pathresolver

import java.io.File

data class ResolvedPaths(
    val envFile: File,
    val secretsDir: File
)