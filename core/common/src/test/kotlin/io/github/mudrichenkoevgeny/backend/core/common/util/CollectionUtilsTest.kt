package io.github.mudrichenkoevgeny.backend.core.common.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CollectionUtilsTest {

    @Test
    fun `all args null returns true`() {
        val args: Array<Any?> = arrayOf(null, null, null)

        val result = isAllArgsNull(*args)

        assertTrue(result)
    }

    @Test
    fun `arg not null returns false`() {
        val args: Array<Any?> = arrayOf(null, "text", null)

        val result = isAllArgsNull(*args)

        assertFalse(result)
    }

    @Test
    fun `no args returns true`() {
        val result = isAllArgsNull()

        assertTrue(result)
    }

    @Test
    fun `different types including non null returns false`() {
        val args: Array<Any?> = arrayOf(null, 42, Any())

        val result = isAllArgsNull(*args)

        assertFalse(result)
    }

    @Test
    fun `mapToSet transforms list to set`() {
        val list = listOf(1, 2, 2, 3)

        val result = list.mapToSet { it * 10 }

        assertEquals(setOf(10, 20, 30), result)
        assertTrue(result is LinkedHashSet)
    }

    @Test
    fun `mapToSet returns empty set for empty list`() {
        val list = emptyList<Int>()

        val result = list.mapToSet { it.toString() }

        assertTrue(result.isEmpty())
    }

    @Test
    fun `mapToSet maintains order for LinkedHashSet`() {
        val list = listOf("c", "a", "b", "a")

        val result = list.mapToSet { it }

        assertEquals(listOf("c", "a", "b"), result.toList())
    }
}