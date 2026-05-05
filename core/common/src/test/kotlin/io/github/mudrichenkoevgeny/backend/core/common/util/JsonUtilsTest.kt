package io.github.mudrichenkoevgeny.backend.core.common.util

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.uuid.Uuid

class JsonUtilsTest {

    @Test
    fun `toElement converts primitives and uuid`() {
        assertEquals(JsonNull, JsonConverter.toElement(null))
        assertEquals(JsonPrimitive("str"), JsonConverter.toElement("str"))
        assertEquals(JsonPrimitive(1), JsonConverter.toElement(1))
        assertEquals(JsonPrimitive(true), JsonConverter.toElement(true))

        val uuid = Uuid.random()
        val uuidElement = JsonConverter.toElement(uuid)
        assertTrue(uuidElement is JsonPrimitive)
        assertEquals(uuid.toHexDashString(), (uuidElement as JsonPrimitive).content)
    }

    @Test
    fun `toElement converts collections and maps`() {
        val listElement = JsonConverter.toElement(listOf(1, 2))
        assertTrue(listElement is JsonArray)
        assertEquals(2, (listElement as JsonArray).size)

        val mapElement = JsonConverter.toElement(mapOf("a" to 1, "b" to "x"))
        assertTrue(mapElement is JsonObject)
        val jsonObject = mapElement as JsonObject
        assertEquals(JsonPrimitive(1), jsonObject["a"])
        assertEquals(JsonPrimitive("x"), jsonObject["b"])
    }

    @Test
    fun `toElement converts recursively`() {
        val complexData = mapOf(
            "list" to listOf(mapOf("id" to 1))
        )

        val result = JsonConverter.toElement(complexData) as JsonObject
        val list = result["list"] as JsonArray
        val nestedMap = list[0] as JsonObject

        assertEquals(JsonPrimitive(1), nestedMap["id"])
    }

    @Test
    fun `toElement returns JsonElement as is`() {
        val existing = JsonPrimitive("already_json")
        val result = JsonConverter.toElement(existing)

        assertEquals(existing, result)
    }

    @Test
    fun `toJsonObject converts map directly`() {
        val source = mapOf("key" to 100)
        val result = JsonConverter.toJsonObject(source)

        assertEquals(JsonPrimitive(100), result["key"])
    }

    @Test
    fun `toJsonElementMap drops nulls`() {
        val source = mapOf(
            "a" to 1,
            "b" to null,
            "c" to "str"
        )

        val result = source.toJsonElementMap()

        assertEquals(2, result.size)
        assertEquals(setOf("a", "c"), result.keys)
        assertEquals(JsonPrimitive(1), result["a"])
        assertEquals(JsonPrimitive("str"), result["c"])
    }
}