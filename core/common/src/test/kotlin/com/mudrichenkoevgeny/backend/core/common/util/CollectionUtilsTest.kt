/*
package io.github.mudrichenkoevgeny.backend.core.common.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CollectionUtilsTest {

    @Test
    fun `all args null returns true`() {
        // GIVEN
        val args: Array<Any?> = arrayOf(null, null, null)
        // WHEN
        val result = CollectionUtils.isAllArgsNull(*args)
        //THEN
        assertTrue(result)
    }

    @Test
    fun `arg not null returns false`() {
        // GIVEN
        val args: Array<Any?> = arrayOf(null, "text", null)
        // WHEN
        val result = CollectionUtils.isAllArgsNull(*args)
        //THEN
        assertFalse(result)
    }

    @Test
    fun `no args returns true`() {
        // GIVEN + WHEN
        val result = CollectionUtils.isAllArgsNull()
        //THEN
        assertTrue(result)
    }

    @Test
    fun `different types including non null returns false`() {
        // GIVEN
        val args: Array<Any?> = arrayOf(null, 42, Any())
        // WHEN
        val result = CollectionUtils.isAllArgsNull(*args)
        //THEN
        assertFalse(result)
    }
}*/
