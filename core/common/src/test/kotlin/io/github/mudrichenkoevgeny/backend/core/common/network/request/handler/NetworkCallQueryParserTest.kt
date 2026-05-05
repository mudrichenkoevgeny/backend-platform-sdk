package io.github.mudrichenkoevgeny.backend.core.common.network.request.handler

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.ktor.http.Parameters
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.ApplicationRequest
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class NetworkCallQueryParserTest {

    @Test
    fun `firstNonBlankQueryValue returns null when parameter absent`() {
        val call = applicationCallWithQuery(Parameters.build { })

        assertNull(call.firstNonBlankQueryValue("q"))
    }

    @Test
    fun `firstNonBlankQueryValue returns null when only blank values`() {
        val call = applicationCallWithQuery(
            Parameters.build {
                append("q", "  ")
                append("q", "")
            }
        )

        assertNull(call.firstNonBlankQueryValue("q"))
    }

    @Test
    fun `firstNonBlankQueryValue returns first non-blank when multiple keys`() {
        val call = applicationCallWithQuery(
            Parameters.build {
                append("q", "  ")
                append("q", "ok")
            }
        )

        assertEquals("ok", call.firstNonBlankQueryValue("q"))
    }

    @Test
    fun `firstNonBlankQueryValue returns single non-blank value`() {
        val call = applicationCallWithQuery(Parameters.build { append("q", "value") })

        assertEquals("value", call.firstNonBlankQueryValue("q"))
    }

    @Test
    fun `parsePositiveIntQuery returns default when parameter absent`() {
        val call = applicationCallWithQuery(Parameters.build { })

        assertEquals(7, call.parsePositiveIntQuery("page", default = 7))
    }

    @Test
    fun `parsePositiveIntQuery returns parsed positive int`() {
        val call = applicationCallWithQuery(Parameters.build { append("page", "3") })

        assertEquals(3, call.parsePositiveIntQuery("page", default = 1))
    }

    @Test
    fun `parsePositiveIntQuery skips leading blank duplicate values`() {
        val call = applicationCallWithQuery(
            Parameters.build {
                append("page", " ")
                append("page", "4")
            }
        )

        assertEquals(4, call.parsePositiveIntQuery("page", default = 1))
    }

    @Test
    fun `parsePositiveIntQuery throws when value is not int`() {
        val call = applicationCallWithQuery(Parameters.build { append("page", "x") })

        val ex = assertThrows(RequestHandlingException::class.java) {
            call.parsePositiveIntQuery("page", default = 1)
        }
        assertEquals(CommonError.InvalidParameterValue::class, ex.error::class)
    }

    @Test
    fun `parsePositiveIntQuery throws when value is zero`() {
        val call = applicationCallWithQuery(Parameters.build { append("page", "0") })

        assertThrows(RequestHandlingException::class.java) {
            call.parsePositiveIntQuery("page", default = 1)
        }
    }

    @Test
    fun `parsePositiveIntQuery throws when value is negative`() {
        val call = applicationCallWithQuery(Parameters.build { append("page", "-1") })

        assertThrows(RequestHandlingException::class.java) {
            call.parsePositiveIntQuery("page", default = 1)
        }
    }

    private fun applicationCallWithQuery(queryParameters: Parameters): ApplicationCall {
        val request = mockk<ApplicationRequest>()
        every { request.queryParameters } returns queryParameters
        val call = mockk<ApplicationCall>()
        every { call.request } returns request
        return call
    }
}
