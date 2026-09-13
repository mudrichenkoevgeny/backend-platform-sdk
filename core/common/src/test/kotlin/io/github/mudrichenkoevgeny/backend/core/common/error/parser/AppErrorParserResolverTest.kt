package io.github.mudrichenkoevgeny.backend.core.common.error.parser

import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.error.model.ErrorId
import io.github.mudrichenkoevgeny.backend.core.common.error.model.createTestAppError
import io.github.mudrichenkoevgeny.shared.foundation.core.common.error.model.ApiErrorResponse
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Unit tests for [AppErrorParserResolver].
 *
 * Verifies that error resolution delegates to specific parsers in sequence and falls back to
 * [CommonErrorParser] when no specific parser handles the error.
 */
class AppErrorParserResolverTest {

    private val commonParser = mockk<CommonErrorParser>()
    private val specificParser1 = mockk<AppErrorSpecificParser>()
    private val specificParser2 = mockk<AppErrorSpecificParser>()

    private val resolver = AppErrorParserResolver(
        commonParser = commonParser,
        specificParsers = setOf(specificParser1, specificParser2)
    )

    @Test
    fun `getApiErrorResponse by code delegates to specific parser when match found`() {
        val errorId = ErrorId.generate()
        val expectedResponse = ApiErrorResponse(
            id = errorId.asHexDashString(),
            code = "SPECIFIC_CODE",
            message = "Specific error message",
            args = emptyMap()
        )

        every { specificParser1.getApiErrorResponse(errorId, "SPECIFIC_CODE", null, "en") } returns expectedResponse

        val response = resolver.getApiErrorResponse(errorId, "SPECIFIC_CODE", null, "en")

        assertEquals(expectedResponse, response)
        verify(exactly = 0) { specificParser2.getApiErrorResponse(any(), any(), any(), any()) }
        verify(exactly = 0) { commonParser.getApiErrorResponse(any(), any(), any(), any()) }
    }

    @Test
    fun `getApiErrorResponse by code falls back to commonParser when no specific parser matches`() {
        val errorId = ErrorId.generate()
        val expectedResponse = ApiErrorResponse(
            id = errorId.asHexDashString(),
            code = "COMMON_CODE",
            message = "Common error message",
            args = emptyMap()
        )

        every { specificParser1.getApiErrorResponse(errorId, "COMMON_CODE", null, "en") } returns null
        every { specificParser2.getApiErrorResponse(errorId, "COMMON_CODE", null, "en") } returns null
        every { commonParser.getApiErrorResponse(errorId, "COMMON_CODE", null, "en") } returns expectedResponse

        val response = resolver.getApiErrorResponse(errorId, "COMMON_CODE", null, "en")

        assertEquals(expectedResponse, response)
        verify(exactly = 1) { commonParser.getApiErrorResponse(errorId, "COMMON_CODE", null, "en") }
    }

    @Test
    fun `getApiErrorResponse by AppError delegates to specific parser when match found`() {
        val appError = createTestAppError(code = "SPECIFIC_CODE")
        val expectedResponse = ApiErrorResponse(
            id = appError.errorId.asHexDashString(),
            code = "SPECIFIC_CODE",
            message = "Specific error message",
            args = emptyMap()
        )

        every { specificParser1.getApiErrorResponse(appError, "en") } returns expectedResponse

        val response = resolver.getApiErrorResponse(appError, "en")

        assertEquals(expectedResponse, response)
        verify(exactly = 0) { specificParser2.getApiErrorResponse(any<AppError>(), any()) }
        verify(exactly = 0) { commonParser.getApiErrorResponse(any<AppError>(), any()) }
    }

    @Test
    fun `getApiErrorResponse by AppError falls back to commonParser when no specific parser matches`() {
        val appError = createTestAppError(code = "COMMON_CODE")
        val expectedResponse = ApiErrorResponse(
            id = appError.errorId.asHexDashString(),
            code = "COMMON_CODE",
            message = "Common error message",
            args = emptyMap()
        )

        every { specificParser1.getApiErrorResponse(appError, "en") } returns null
        every { specificParser2.getApiErrorResponse(appError, "en") } returns null
        every { commonParser.getApiErrorResponse(appError, "en") } returns expectedResponse

        val response = resolver.getApiErrorResponse(appError, "en")

        assertEquals(expectedResponse, response)
        verify(exactly = 1) { commonParser.getApiErrorResponse(appError, "en") }
    }
}
