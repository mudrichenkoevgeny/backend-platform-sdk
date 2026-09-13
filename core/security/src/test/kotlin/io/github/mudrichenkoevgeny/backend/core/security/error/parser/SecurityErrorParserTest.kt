package io.github.mudrichenkoevgeny.backend.core.security.error.parser

import io.github.mudrichenkoevgeny.backend.core.common.error.model.ErrorId
import io.github.mudrichenkoevgeny.backend.core.common.error.model.createTestAppError
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Unit tests for [SecurityErrorParser].
 *
 * Verifies that [SecurityErrorParser] template parser currently returns null for all requests,
 * allowing resolution to delegate further down the chain.
 */
class SecurityErrorParserTest {

    private val parser = SecurityErrorParser()

    @Test
    fun `getApiErrorResponse by code returns null`() {
        val response = parser.getApiErrorResponse(
            errorId = ErrorId.generate(),
            code = "SOME_SECURITY_CODE",
            args = null,
            locale = "en"
        )
        assertNull(response)
    }

    @Test
    fun `getApiErrorResponse by AppError returns null`() {
        val appError = createTestAppError(code = "SOME_SECURITY_CODE")

        val response = parser.getApiErrorResponse(appError, "en")

        assertNull(response)
    }
}
