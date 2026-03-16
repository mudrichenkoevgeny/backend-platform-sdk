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
        assertEquals(listOf(JsonPrimitive(1), JsonPrimitive(2)), listElement)

        val mapElement = JsonConverter.toElement(mapOf("a" to 1, "b" to "x"))
        assertTrue(mapElement is JsonObject)
        val jsonObject = mapElement as JsonObject
        assertEquals(JsonPrimitive(1), jsonObject["a"])
        assertEquals(JsonPrimitive("x"), jsonObject["b"])
    }

    @Test
    fun `toJsonElementMap drops nulls`() {
        val source = mapOf(
            "a" to 1,
            "b" to null,
            "c" to "str",
        )

        val result = source.toJsonElementMap()

        assertEquals(setOf("a", "c"), result.keys)
        assertEquals(JsonPrimitive(1), result["a"])
        assertEquals(JsonPrimitive("str"), result["c"])
    }
}

