package io.github.mudrichenkoevgeny.backend.core.common.util

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.uuid.Uuid

/**
 * Helpers for converting arbitrary Kotlin values into kotlinx.serialization [JsonElement] trees.
 */
object JsonConverter {

    /**
     * Converts a single [value] into a [JsonElement] using the following rules:
     * - `null` → [JsonNull]
     * - [JsonElement] → returned as is
     * - [String], [Number], [Boolean] → [JsonPrimitive]
     * - [Uuid] → [JsonPrimitive] with its hex‑dash representation
     * - [Iterable] → [JsonArray] with each element converted recursively
     * - [Map] → [JsonObject] with keys converted to strings and values converted recursively
     * - any other type → [JsonPrimitive] with `value.toString()`
     */
    fun toElement(value: Any?): JsonElement = when (value) {
        null -> JsonNull
        is JsonElement -> value
        is String -> JsonPrimitive(value)
        is Number -> JsonPrimitive(value)
        is Boolean -> JsonPrimitive(value)
        is Uuid -> JsonPrimitive(value.toHexDashString())
        is Iterable<*> -> JsonArray(value.map { toElement(it) })
        is Map<*, *> -> JsonObject(value.map { it.key.toString() to toElement(it.value) }.toMap())
        else -> JsonPrimitive(value.toString())
    }

    /**
     * Converts a [map] of arbitrary values into a [JsonObject] using [toElement] for each entry.
     */
    fun toJsonObject(map: Map<String, Any?>): JsonObject {
        return JsonObject(map.mapValues { toElement(it.value) })
    }
}

/**
 * Converts a map of arbitrary values into a map with only non‑null JSON elements.
 *
 * Entries whose value would result in `null` are dropped.
 */
fun Map<String, Any?>.toJsonElementMap(): Map<String, JsonElement> =
    mapNotNull { (key, value) ->
        val jsonValue = value?.let { JsonConverter.toElement(it) }
        jsonValue?.let { key to it }
    }.toMap()

