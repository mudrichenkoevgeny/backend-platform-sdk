package io.github.mudrichenkoevgeny.backend.core.security.error.parser

import io.github.mudrichenkoevgeny.backend.core.common.error.model.ErrorId
import io.github.mudrichenkoevgeny.backend.core.common.error.model.createTestAppError
import io.github.mudrichenkoevgeny.shared.foundation.core.security.error.naming.SecurityErrorCodes
import io.github.mudrichenkoevgeny.shared.foundation.core.security.localization.SecurityModuleStrings
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Unit tests for [SecurityErrorParser].
 *
 * Verifies that [SecurityErrorParser] resolves security error codes via [SecurityModuleStrings]
 * and returns null for unhandled codes.
 */
class SecurityErrorParserTest {

    private val parser = SecurityErrorParser()
    private val stringsEn = SecurityModuleStrings(locale = "en")
    private val stringsRu = SecurityModuleStrings(locale = "ru")

    @Test
    fun `getApiErrorResponse by code returns localized message for TOTP_ALREADY_ENABLED`() {
        val errorId = ErrorId.generate()
        val response = parser.getApiErrorResponse(
            errorId = errorId,
            code = SecurityErrorCodes.TOTP_ALREADY_ENABLED,
            args = null,
            locale = "en"
        )
        assertNotNull(response)
        assertEquals(errorId.asHexDashString(), response!!.id)
        assertEquals(SecurityErrorCodes.TOTP_ALREADY_ENABLED, response.code)
        assertEquals(stringsEn.errors.totpAlreadyEnabled, response.message)
    }

    @Test
    fun `getApiErrorResponse by code returns Russian message when requested`() {
        val response = parser.getApiErrorResponse(
            errorId = ErrorId.generate(),
            code = SecurityErrorCodes.TOTP_ALREADY_ENABLED,
            args = null,
            locale = "ru"
        )
        assertNotNull(response)
        assertEquals(stringsRu.errors.totpAlreadyEnabled, response!!.message)
    }

    @Test
    fun `getApiErrorResponse returns null for unhandled error code`() {
        val appError = createTestAppError(code = "UNKNOWN_NON_SECURITY_CODE")

        val response = parser.getApiErrorResponse(appError, "en")

        assertNull(response)
    }
}