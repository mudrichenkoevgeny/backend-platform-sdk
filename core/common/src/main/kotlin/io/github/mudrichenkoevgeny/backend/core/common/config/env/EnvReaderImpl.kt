package io.github.mudrichenkoevgeny.backend.core.common.config.env

import io.github.cdimascio.dotenv.Dotenv
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EnvReaderImpl @Inject constructor(
    private val dotenv: Dotenv,
    private val secretsRoot: File
): EnvReader {

    override fun getByKey(key: String): String = dotenv[key] ?: error("Environment variable '$key' not found.")

    override fun getByKeyOrNull(key: String): String? = dotenv[key]

    override fun readSecret(relativeFile: String): String {
        val rawFile = File(relativeFile)

        val secretFile = if (rawFile.isAbsolute) {
            rawFile
        } else {
            File(secretsRoot, relativeFile)
        }

        println("DEBUG: Reading secret from: ${secretFile.absolutePath}")

        if (!secretFile.exists()) {
            error("Secret file not found at: ${secretFile.absolutePath}")
        }

        return secretFile.readText().trim()
    }
}