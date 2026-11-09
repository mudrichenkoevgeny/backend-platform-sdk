package io.github.mudrichenkoevgeny.backend.core.common.config.env

import io.github.mudrichenkoevgeny.shared.foundation.core.common.serialization.FoundationJson
import kotlinx.serialization.json.Json

/**
 * Reads a delimited environment variable and converts it into a list of non-empty, trimmed strings.
 *
 * @param key environment variable name.
 * @param delimiter separator used between values, defaults to a comma.
 * @return list of values or an empty list when the variable is missing or blank.
 */
public fun EnvReader.getStringList(
    key: String,
    delimiter: String = ","
): List<String> = getByKeyOrNull(key)?.split(delimiter)?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()

/**
 * Reads a secret file and deserializes its JSON contents into the requested type [T].
 *
 * @param relativeFile relative or absolute path to the secret file.
 * @param json [Json] instance used for deserialization, defaults to [FoundationJson].
 * @return decoded value of type [T].
 */
public inline fun <reified T> EnvReader.readJsonSecret(
    relativeFile: String,
    json: Json = FoundationJson
): T {
    val rawContent = this.readSecret(relativeFile)
    return json.decodeFromString<T>(rawContent)
}
