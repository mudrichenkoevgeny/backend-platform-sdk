package io.github.mudrichenkoevgeny.backend.core.common.network.request.handler

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.ktor.http.parametersOf
import io.ktor.server.application.ApplicationCall
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class NetworkCallPathValidatorTest {

    @Test
    fun `validatePathParameter returns mapped value when present and valid`() {
        val call = mockk<ApplicationCall> {
            every { parameters } returns parametersOf("id", "42")
        }

        val id: Int = call.validatePathParameter("id") { it.toInt() }

        assertEquals(42, id)
    }

    @Test
    fun `validatePathParameter throws MissingRequiredParameter when absent`() {
        val call = mockk<ApplicationCall> {
            every { parameters } returns parametersOf()
        }

        val ex = assertThrows(RequestHandlingException::class.java) {
            call.validatePathParameter("id") { it.toInt() }
        }

        assertEquals(CommonError.MissingRequiredParameter::class, ex.error::class)
    }

    @Test
    fun `validatePathParameter throws InvalidParameterValue when mapper fails`() {
        val call = mockk<ApplicationCall> {
            every { parameters } returns parametersOf("id", "not-int")
        }

        val ex = assertThrows(RequestHandlingException::class.java) {
            call.validatePathParameter("id") { it.toInt() }
        }

        assertEquals(CommonError.InvalidParameterValue::class, ex.error::class)
    }
}

