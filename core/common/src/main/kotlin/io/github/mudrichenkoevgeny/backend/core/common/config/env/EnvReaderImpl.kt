package io.github.mudrichenkoevgeny.backend.core.common.config.env

import io.github.mudrichenkoevgeny.backend.core.common.config.pathresolver.ResolvedPaths
import java.io.File
import java.util.Properties
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EnvReaderImpl @Inject constructor(
    private val paths: ResolvedPaths
): EnvReader {

    private val fileVariables: Map<String, String> by lazy {
        val props = Properties()
        if (paths.envFile.exists()) {
            paths.envFile.inputStream().use { props.load(it) }
        }
        props.map { it.key.toString() to it.value.toString() }.toMap()
    }

    override fun getByKey(key: String): String {
        return getByKeyOrNull(key)
            ?: error("Environment variable '$key' not found.")
    }

    override fun getByKeyOrNull(key: String): String? {
        return System.getenv(key) ?: fileVariables[key]
    }

    override fun readSecret(relativeFile: String): String {
        val secretFile = if (File(relativeFile).isAbsolute) {
            File(relativeFile)
        } else {
            File(paths.secretsDir, relativeFile)
        }

        if (!secretFile.exists()) {
            error("Secret file not found at: ${secretFile.absolutePath}")
        }

        return secretFile.readText().trim()
    }
}