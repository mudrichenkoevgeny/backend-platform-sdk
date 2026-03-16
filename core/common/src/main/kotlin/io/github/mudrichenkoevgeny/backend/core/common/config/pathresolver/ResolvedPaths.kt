package io.github.mudrichenkoevgeny.backend.core.common.config.pathresolver

import java.io.File
import io.github.mudrichenkoevgeny.backend.core.common.config.env.EnvReader

/**
 * Resolved filesystem locations used to load environment variables and secrets.
 *
 * Both paths are expected to be absolute and point to files/directories that are readable
 * by the running process.
 *
 * @param envFile file with key/value environment overrides (e.g. `.env`).
 * @param secretsDir directory that contains secret files referenced by [EnvReader.readSecret].
 */
data class ResolvedPaths(
    val envFile: File,
    val secretsDir: File
)