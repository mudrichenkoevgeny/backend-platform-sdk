package io.github.mudrichenkoevgeny.backend.feature.user.error.parser

import io.github.mudrichenkoevgeny.backend.core.common.error.model.ErrorId
import io.github.mudrichenkoevgeny.backend.core.common.error.model.createTestAppError
import io.github.mudrichenkoevgeny.backend.core.common.error.parser.CommonErrorParser
import io.github.mudrichenkoevgeny.backend.core.common.util.formatEpochMillisToUtcString
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.shared.foundation.core.common.error.model.ApiErrorResponse
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.accountlockout.AccountLockoutType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.error.naming.UserErrorArgs
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.error.naming.UserErrorCodes
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import kotlin.time.Instant

/**
 * Unit tests for [UserErrorParser].
 *
 * Verifies that [UserErrorParser]:
 * - Returns null for error code and args based lookup.
 * - Correctly formats `TEMPORARY_LOCKOUT_UNTIL` epoch millis into a UTC date string when processing `USER_LOCKED` errors.
 * - Returns null when error is not `USER_LOCKED` or lacks `TEMPORARY_LOCKOUT_UNTIL`.
 */
class UserErrorParserTest {

    private val commonParser = mockk<CommonErrorParser>()
    private val parser = UserErrorParser(commonParser)

    @Test
    fun `getApiErrorResponse by code returns null`() {
        val response = parser.getApiErrorResponse(
            errorId = ErrorId.generate(),
            code = UserErrorCodes.USER_LOCKED,
            args = null,
            locale = "en"
        )
        assertNull(response)
    }

    @Test
    fun `getApiErrorResponse by AppError formats epoch millis and delegates when code is USER_LOCKED`() {
        val lockoutUntilEpoch = 1700000000000L
        val formattedDate = lockoutUntilEpoch.formatEpochMillisToUtcString()
        val appError = UserError.UserLocked(
            lockoutType = AccountLockoutType.TEMPORARY,
            temporaryLockoutUntil = Instant.fromEpochMilliseconds(lockoutUntilEpoch)
        )

        val expectedResponse = ApiErrorResponse(
            id = appError.errorId.asHexDashString(),
            code = UserErrorCodes.USER_LOCKED,
            message = "User is locked until $formattedDate",
            args = mapOf(UserErrorArgs.TEMPORARY_LOCKOUT_UNTIL to formattedDate)
        )

        every {
            commonParser.getApiErrorResponse(
                errorId = appError.errorId,
                code = UserErrorCodes.USER_LOCKED,
                args = match { map -> map[UserErrorArgs.TEMPORARY_LOCKOUT_UNTIL] == formattedDate },
                locale = "en"
            )
        } returns expectedResponse

        val response = parser.getApiErrorResponse(appError, "en")

        assertEquals(expectedResponse, response)
        verify(exactly = 1) {
            commonParser.getApiErrorResponse(
                errorId = appError.errorId,
                code = UserErrorCodes.USER_LOCKED,
                args = match { map -> map[UserErrorArgs.TEMPORARY_LOCKOUT_UNTIL] == formattedDate },
                locale = "en"
            )
        }
    }

    @Test
    fun `getApiErrorResponse by AppError returns null when code is not USER_LOCKED`() {
        val appError = createTestAppError(code = "OTHER_USER_ERROR")

        val response = parser.getApiErrorResponse(appError, "en")

        assertNull(response)
        verify(exactly = 0) { commonParser.getApiErrorResponse(any(), any(), any(), any()) }
    }

    @Test
    fun `getApiErrorResponse by AppError returns null when USER_LOCKED lacks TEMPORARY_LOCKOUT_UNTIL arg`() {
        val appError = createTestAppError(code = UserErrorCodes.USER_LOCKED, publicArgs = emptyMap<String, Any>())

        val response = parser.getApiErrorResponse(appError, "en")

        assertNull(response)
        verify(exactly = 0) { commonParser.getApiErrorResponse(any(), any(), any(), any()) }
    }
}
