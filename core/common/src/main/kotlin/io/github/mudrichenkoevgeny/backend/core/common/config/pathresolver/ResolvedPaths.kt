package io.github.mudrichenkoevgeny.backend.core.common.config.pathresolver

import io.github.mudrichenkoevgeny.backend.core.common.config.env.EnvReader
import java.io.File

/**
 * Resolved filesystem locations used to load environment variables and secrets.
 *
 * Both paths are expected to be absolute and point to files/directories that are readable
 * by the running process.
 *
 * @param envFile file with key/value environment overrides (e.g. `.env`).
 * @param secretsDir directory that contains secret files referenced by [EnvReader.readSecret].
 */
public data class ResolvedPaths(
    val envFile: File,
    val secretsDir: File
)
