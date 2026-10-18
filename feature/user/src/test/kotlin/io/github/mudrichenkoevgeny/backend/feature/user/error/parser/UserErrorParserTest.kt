package io.github.mudrichenkoevgeny.backend.feature.user.error.parser

import io.github.mudrichenkoevgeny.backend.core.common.error.model.ErrorId
import io.github.mudrichenkoevgeny.backend.core.common.error.model.createTestAppError
import io.github.mudrichenkoevgeny.backend.core.common.util.formatEpochMillisToUtcString
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.accountlockout.AccountLockoutType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.error.naming.UserErrorArgs
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.error.naming.UserErrorCodes
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.localization.UserModuleStrings
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import kotlin.time.Instant

/**
 * Unit tests for [UserErrorParser].
 *
 * Verifies that [UserErrorParser]:
 * - Resolves localized messages for user-related error codes using [UserModuleStrings].
 * - Correctly formats `TEMPORARY_LOCKOUT_UNTIL` epoch millis into a UTC date string when processing `USER_LOCKED` errors.
 * - Returns null for non-user error codes to delegate to the next parser in the chain.
 */
class UserErrorParserTest {

    private val parser = UserErrorParser()
    private val stringsEn = UserModuleStrings(locale = "en")
    private val stringsRu = UserModuleStrings(locale = "ru")

    @Test
    fun `getApiErrorResponse by code returns localized response for USER_NOT_FOUND`() {
        val errorId = ErrorId.generate()
        val response = parser.getApiErrorResponse(
            errorId = errorId,
            code = UserErrorCodes.USER_NOT_FOUND,
            args = null,
            locale = "en"
        )
        assertNotNull(response)
        assertEquals(errorId.asHexDashString(), response!!.id)
        assertEquals(UserErrorCodes.USER_NOT_FOUND, response.code)
        assertEquals(stringsEn.errors.notFound, response.message)
    }

    @Test
    fun `getApiErrorResponse by code returns Russian message when requested`() {
        val response = parser.getApiErrorResponse(
            errorId = ErrorId.generate(),
            code = UserErrorCodes.USER_NOT_FOUND,
            args = null,
            locale = "ru"
        )
        assertNotNull(response)
        assertEquals(stringsRu.errors.notFound, response!!.message)
    }

    @Test
    fun `getApiErrorResponse by AppError formats epoch millis when code is USER_LOCKED`() {
        val lockoutUntilEpoch = 1700000000000L
        val formattedDate = lockoutUntilEpoch.formatEpochMillisToUtcString()
        val appError = UserError.UserLocked(
            lockoutType = AccountLockoutType.TEMPORARY,
            temporaryLockoutUntil = Instant.fromEpochMilliseconds(lockoutUntilEpoch)
        )

        val response = parser.getApiErrorResponse(appError, "en")

        assertNotNull(response)
        assertEquals(appError.errorId.asHexDashString(), response!!.id)
        assertEquals(UserErrorCodes.USER_LOCKED, response.code)
        assertEquals(stringsEn.errors.locked(formattedDate), response.message)
        assertEquals(formattedDate, response.args[UserErrorArgs.TEMPORARY_LOCKOUT_UNTIL])
    }

    @Test
    fun `getApiErrorResponse returns null when code is not handled by UserErrorParser`() {
        val appError = createTestAppError(code = "COMMON_UNKNOWN_CODE")

        val response = parser.getApiErrorResponse(appError, "en")

        assertNull(response)
    }
}