package io.github.mudrichenkoevgeny.backend.core.common.network.request.handler

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.ktor.http.Parameters
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class NetworkCallQueryParserTest {

    @Test
    fun `firstNonBlankQueryValue returns null when parameter absent`() {
        val call = createTestApplicationCallWithQuery(Parameters.build { })

        assertNull(call.firstNonBlankQueryValue("q"))
    }

    @Test
    fun `firstNonBlankQueryValue returns null when only blank values`() {
        val call = createTestApplicationCallWithQuery(
            Parameters.build {
                append("q", "  ")
                append("q", "")
            }
        )

        assertNull(call.firstNonBlankQueryValue("q"))
    }

    @Test
    fun `firstNonBlankQueryValue returns first non-blank when multiple keys`() {
        val call = createTestApplicationCallWithQuery(
            Parameters.build {
                append("q", "  ")
                append("q", "ok")
            }
        )

        assertEquals("ok", call.firstNonBlankQueryValue("q"))
    }

    @Test
    fun `firstNonBlankQueryValue returns single non-blank value`() {
        val call = createTestApplicationCallWithQuery(Parameters.build { append("q", "value") })

        assertEquals("value", call.firstNonBlankQueryValue("q"))
    }

    @Test
    fun `parsePositiveIntQuery returns default when parameter absent`() {
        val call = createTestApplicationCallWithQuery(Parameters.build { })

        assertEquals(7, call.parsePositiveIntQuery("page", default = 7))
    }

    @Test
    fun `parsePositiveIntQuery returns parsed positive int`() {
        val call = createTestApplicationCallWithQuery(Parameters.build { append("page", "3") })

        assertEquals(3, call.parsePositiveIntQuery("page", default = 1))
    }

    @Test
    fun `parsePositiveIntQuery skips leading blank duplicate values`() {
        val call = createTestApplicationCallWithQuery(
            Parameters.build {
                append("page", " ")
                append("page", "4")
            }
        )

        assertEquals(4, call.parsePositiveIntQuery("page", default = 1))
    }

    @Test
    fun `parsePositiveIntQuery throws when value is not int`() {
        val call = createTestApplicationCallWithQuery(Parameters.build { append("page", "x") })

        val ex = assertThrows(RequestHandlingException::class.java) {
            call.parsePositiveIntQuery("page", default = 1)
        }
        assertEquals(CommonError.InvalidParameterValue::class, ex.error::class)
    }

    @Test
    fun `parsePositiveIntQuery throws when value is zero`() {
        val call = createTestApplicationCallWithQuery(Parameters.build { append("page", "0") })

        assertThrows(RequestHandlingException::class.java) {
            call.parsePositiveIntQuery("page", default = 1)
        }
    }

    @Test
    fun `parsePositiveIntQuery throws when value is negative`() {
        val call = createTestApplicationCallWithQuery(Parameters.build { append("page", "-1") })

        assertThrows(RequestHandlingException::class.java) {
            call.parsePositiveIntQuery("page", default = 1)
        }
    }

    @Test
    fun `getQueryValues returns empty list when parameter absent`() {
        val call = createTestApplicationCallWithQuery(Parameters.build { })

        assertEquals(emptyList<String>(), call.getQueryValues("filter"))
    }

    @Test
    fun `getQueryValues filters out blank values and returns non-blank list`() {
        val call = createTestApplicationCallWithQuery(
            Parameters.build {
                append("filter", " ")
                append("filter", "val1")
                append("filter", "")
                append("filter", "val2")
            }
        )

        assertEquals(listOf("val1", "val2"), call.getQueryValues("filter"))
    }

    @Test
    fun `parseValidatedQueryList returns empty list when parameter absent`() {
        val call = createTestApplicationCallWithQuery(Parameters.build { })

        val result = call.parseValidatedQueryList("status") { raw -> raw.uppercase() }

        assertEquals(emptyList<String>(), result)
    }

    @Test
    fun `parseValidatedQueryList returns parsed list when all values valid`() {
        val call = createTestApplicationCallWithQuery(
            Parameters.build {
                append("status", "active")
                append("status", "banned")
            }
        )

        val result = call.parseValidatedQueryList("status") { raw -> raw.uppercase() }

        assertEquals(listOf("ACTIVE", "BANNED"), result)
    }

    @Test
    fun `parseValidatedQueryList throws RequestHandlingException when any value is invalid`() {
        val call = createTestApplicationCallWithQuery(
            Parameters.build {
                append("status", "active")
                append("status", "invalid_status")
            }
        )

        val ex = assertThrows(RequestHandlingException::class.java) {
            call.parseValidatedQueryList("status") { raw ->
                if (raw == "active") "ACTIVE" else null
            }
        }

        assertEquals(CommonError.InvalidParameterValue::class, ex.error::class)
        assertEquals("status", (ex.error as CommonError.InvalidParameterValue).parameterName)
    }

    @Test
    fun `parseBooleanQueryOrNull returns null when parameter absent`() {
        val call = createTestApplicationCallWithQuery(Parameters.build { })

        assertNull(call.parseBooleanQueryOrNull("flag"))
    }

    @Test
    fun `parseBooleanQueryOrNull returns boolean for true and false strings`() {
        val callTrue = createTestApplicationCallWithQuery(Parameters.build { append("flag", "true") })
        val callFalse = createTestApplicationCallWithQuery(Parameters.build { append("flag", "false") })

        assertEquals(true, callTrue.parseBooleanQueryOrNull("flag"))
        assertEquals(false, callFalse.parseBooleanQueryOrNull("flag"))
    }

    @Test
    fun `parseBooleanQueryOrNull returns null for invalid non-boolean string`() {
        val call = createTestApplicationCallWithQuery(Parameters.build { append("flag", "1") })

        assertNull(call.parseBooleanQueryOrNull("flag"))
    }

    @Test
    fun `parseIntQueryOrNull returns null when parameter absent`() {
        val call = createTestApplicationCallWithQuery(Parameters.build { })

        assertNull(call.parseIntQueryOrNull("level"))
    }

    @Test
    fun `parseIntQueryOrNull returns parsed int when valid`() {
        val call = createTestApplicationCallWithQuery(Parameters.build { append("level", "42") })

        assertEquals(42, call.parseIntQueryOrNull("level"))
    }

    @Test
    fun `parseIntQueryOrNull returns null when non-numeric`() {
        val call = createTestApplicationCallWithQuery(Parameters.build { append("level", "abc") })

        assertNull(call.parseIntQueryOrNull("level"))
    }

    @Test
    fun `parseLongQueryOrNull returns null when parameter absent`() {
        val call = createTestApplicationCallWithQuery(Parameters.build { })

        assertNull(call.parseLongQueryOrNull("bytes"))
    }

    @Test
    fun `parseLongQueryOrNull returns parsed long when valid`() {
        val call = createTestApplicationCallWithQuery(Parameters.build { append("bytes", "10737418240") })

        assertEquals(10737418240L, call.parseLongQueryOrNull("bytes"))
    }

    @Test
    fun `parseLongQueryOrNull returns null when non-numeric`() {
        val call = createTestApplicationCallWithQuery(Parameters.build { append("bytes", "xyz") })

        assertNull(call.parseLongQueryOrNull("bytes"))
    }

    @Test
    fun `parseTimestampQueryOrNull returns null when parameter absent`() {
        val call = createTestApplicationCallWithQuery(Parameters.build { })

        assertNull(call.parseTimestampQueryOrNull("created_at"))
    }

    @Test
    fun `parseTimestampQueryOrNull returns parsed Instant when valid ISO-8601 string`() {
        val call = createTestApplicationCallWithQuery(
            Parameters.build { append("created_at", "2026-01-01T12:00:00Z") }
        )

        val result = call.parseTimestampQueryOrNull("created_at")

        assertEquals(kotlin.time.Instant.parse("2026-01-01T12:00:00Z"), result)
    }

    @Test
    fun `parseTimestampQueryOrNull throws RequestHandlingException when invalid timestamp string`() {
        val call = createTestApplicationCallWithQuery(
            Parameters.build { append("created_at", "invalid-timestamp") }
        )

        val ex = assertThrows(RequestHandlingException::class.java) {
            call.parseTimestampQueryOrNull("created_at")
        }

        assertEquals(CommonError.InvalidParameterValue::class, ex.error::class)
        assertEquals("created_at", (ex.error as CommonError.InvalidParameterValue).parameterName)
    }
}
