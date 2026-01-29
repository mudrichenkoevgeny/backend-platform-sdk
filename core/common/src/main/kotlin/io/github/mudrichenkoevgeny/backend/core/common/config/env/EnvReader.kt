package io.github.mudrichenkoevgeny.backend.core.common.config.env

interface EnvReader {
    fun getByKey(key: String): String
    fun getByKeyOrNull(key: String): String?
    fun readSecret(relativeFile: String): String
}