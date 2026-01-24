package com.mudrichenkoevgeny.backend.core.common.util

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.util.UUID

object JsonConverter {
    fun toElement(value: Any?): JsonElement = when (value) {
        null -> JsonNull
        is JsonElement -> value
        is String -> JsonPrimitive(value)
        is Number -> JsonPrimitive(value)
        is Boolean -> JsonPrimitive(value)
        is UUID -> JsonPrimitive(value.toString())
        is Iterable<*> -> JsonArray(value.map { toElement(it) })
        is Map<*, *> -> JsonObject(value.map { it.key.toString() to toElement(it.value) }.toMap())
        else -> JsonPrimitive(value.toString())
    }

    fun toJsonObject(map: Map<String, Any?>): JsonObject {
        return JsonObject(map.mapValues { toElement(it.value) })
    }
}

fun Map<String, Any?>.toJsonElementMap(): Map<String, JsonElement> =
    mapNotNull { (key, value) ->
        val jsonValue = value?.let { JsonConverter.toElement(it) }
        jsonValue?.let { key to it }
    }.toMap()