package io.github.mudrichenkoevgeny.backend.core.common.config.env

import io.github.mudrichenkoevgeny.shared.foundation.core.common.serialization.FoundationJson
import kotlinx.serialization.json.Json

inline fun <reified T> EnvReader.readJsonSecret(
    relativeFile: String,
    json: Json = FoundationJson
): T {
    val rawContent = this.readSecret(relativeFile)
    return json.decodeFromString<T>(rawContent)
}