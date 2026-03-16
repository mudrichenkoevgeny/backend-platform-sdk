package io.github.mudrichenkoevgeny.backend.core.common.util

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CollectionUtilsTest {

    @Test
    fun `all args null returns true`() {
        val args: Array<Any?> = arrayOf(null, null, null)

        val result = CollectionUtils.isAllArgsNull(*args)

        assertTrue(result)
    }

    @Test
    fun `arg not null returns false`() {
        val args: Array<Any?> = arrayOf(null, "text", null)

        val result = CollectionUtils.isAllArgsNull(*args)

        assertFalse(result)
    }

    @Test
    fun `no args returns true`() {
        val result = CollectionUtils.isAllArgsNull()

        assertTrue(result)
    }

    @Test
    fun `different types including non null returns false`() {
        val args: Array<Any?> = arrayOf(null, 42, Any())

        val result = CollectionUtils.isAllArgsNull(*args)

        assertFalse(result)
    }
}