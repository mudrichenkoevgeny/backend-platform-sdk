package io.github.mudrichenkoevgeny.backend.core.common.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Instant as JavaInstant
import kotlin.time.Instant as KotlinInstant

class TimeUtilsTest {

    @Test
    fun `toJavaInstant converts correctly`() {
        val millis = 1714730000000L
        val kotlinInstant = KotlinInstant.fromEpochMilliseconds(millis)

        val result = kotlinInstant.toJavaInstant()

        assertEquals(millis, result.toEpochMilli())
    }

    @Test
    fun `toJavaInstantOrNull handles null and value`() {
        val millis = 1714730000000L
        val kotlinInstant: KotlinInstant = KotlinInstant.fromEpochMilliseconds(millis)
        val nullInstant: KotlinInstant? = null

        assertEquals(millis, kotlinInstant.toJavaInstantOrNull()?.toEpochMilli())
        assertNull(nullInstant.toJavaInstantOrNull())
    }

    @Test
    fun `toKotlinInstant converts correctly`() {
        val millis = 1714730000000L
        val javaInstant = JavaInstant.ofEpochMilli(millis)

        val result = javaInstant.toKotlinInstant()

        assertEquals(millis, result.toEpochMilliseconds())
    }

    @Test
    fun `toKotlinInstantOrNull handles null and value`() {
        val millis = 1714730000000L
        val javaInstant: JavaInstant? = JavaInstant.ofEpochMilli(millis)
        val nullInstant: JavaInstant? = null

        assertEquals(millis, javaInstant.toKotlinInstantOrNull()?.toEpochMilliseconds())
        assertNull(nullInstant.toKotlinInstantOrNull())
    }
}