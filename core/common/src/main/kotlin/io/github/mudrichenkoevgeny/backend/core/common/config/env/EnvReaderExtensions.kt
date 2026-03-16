package io.github.mudrichenkoevgeny.backend.core.common.config.env

import io.github.mudrichenkoevgeny.shared.foundation.core.common.serialization.FoundationJson
import kotlinx.serialization.json.Json

/**
 * Reads a secret file and deserializes its JSON contents into the requested type [T].
 *
 * @param relativeFile relative or absolute path to the secret file.
 * @param json [Json] instance used for deserialization, defaults to [FoundationJson].
 * @return decoded value of type [T].
 */
inline fun <reified T> EnvReader.readJsonSecret(
    relativeFile: String,
    json: Json = FoundationJson
): T {
    val rawContent = this.readSecret(relativeFile)
    return json.decodeFromString<T>(rawContent)
}