package io.github.mudrichenkoevgeny.backend.core.database.extensions

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SubstringSqlLikePatternTest {

    @Test
    fun `substringSqlLikePattern builds simple pattern`() {
        val needle = "fragment"
        val pattern = substringSqlLikePattern(needle)

        assertEquals("%fragment%", pattern.pattern)
    }

    @Test
    fun `substringSqlLikePattern escapes percent sign`() {
        val needle = "100%"
        val pattern = substringSqlLikePattern(needle)

        assertEquals("%100\\%%", pattern.pattern)
    }

    @Test
    fun `substringSqlLikePattern escapes underscore sign`() {
        val needle = "user_name"
        val pattern = substringSqlLikePattern(needle)

        assertEquals("%user\\_name%", pattern.pattern)
    }

    @Test
    fun `substringSqlLikePattern escapes backslash`() {
        val needle = "path\\to"
        val pattern = substringSqlLikePattern(needle)

        assertEquals("%path\\\\to%", pattern.pattern)
    }

    @Test
    fun `substringSqlLikePattern handles mixed special characters`() {
        val needle = "%_\\"
        val pattern = substringSqlLikePattern(needle)

        assertEquals("%\\%\\_\\\\%", pattern.pattern)
    }

    @Test
    fun `substringSqlLikePattern uses custom escape character`() {
        val needle = "100%"
        val escapeChar = '!'
        val pattern = substringSqlLikePattern(needle, escapeChar)

        assertEquals("%100\\%%", pattern.pattern)
        assertEquals(escapeChar, pattern.escapeChar)
    }
}