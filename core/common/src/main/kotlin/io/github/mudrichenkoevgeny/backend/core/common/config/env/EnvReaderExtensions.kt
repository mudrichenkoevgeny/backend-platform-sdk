package io.github.mudrichenkoevgeny.backend.core.common.config.env

import io.github.mudrichenkoevgeny.backend.core.common.serialization.DefaultJson
import kotlinx.serialization.json.Json

inline fun <reified T> EnvReader.readJsonSecret(
    relativeFile: String,
    json: Json = DefaultJson
): T {
    val rawContent = this.readSecret(relativeFile)
    return json.decodeFromString<T>(rawContent)
}