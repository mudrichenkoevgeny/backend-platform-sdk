/*
package io.github.mudrichenkoevgeny.backend.core.common.util

import io.github.mudrichenkoevgeny.backend.core.common.util.TimeUtils.toInstantUTC
import io.github.mudrichenkoevgeny.backend.core.common.util.TimeUtils.toLocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDateTime

class TimeUtilsTest {

    @Test
    fun `LocalDateTime to InstantUTC and back`() {
        // GIVEN
        val localDateTime = LocalDateTime.of(2025, 10, 27, 15, 30, 45)
        val instant = localDateTime.toInstantUTC()

        // WHEN
        val converted = instant.toLocalDateTime()

        // THEN
        assertEquals(localDateTime, converted)
    }

    @Test
    fun `Instant to LocalDateTime and back`() {
        // GIVEN
        val instant = Instant.parse("2025-10-27T15:30:45Z")
        val localDateTime = instant.toLocalDateTime()

        // WHEN
        val converted = localDateTime.toInstantUTC()

        // THEN
        assertEquals(instant, converted)
    }
}
*/
