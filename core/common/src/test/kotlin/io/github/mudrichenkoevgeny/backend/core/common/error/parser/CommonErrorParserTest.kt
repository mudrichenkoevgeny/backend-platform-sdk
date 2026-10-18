package io.github.mudrichenkoevgeny.backend.core.common.error.parser

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.error.model.ErrorId
import io.github.mudrichenkoevgeny.shared.foundation.core.common.error.naming.CommonErrorCodes
import io.github.mudrichenkoevgeny.shared.foundation.core.common.localization.CommonModuleStrings
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Unit tests for fallback [CommonErrorParser].
 *
 * Verifies fallback error message resolution via `shared-foundation` [CommonModuleStrings].
 * Validates locale normalization and fallback behavior.
 */
class CommonErrorParserTest {

    private val parser = CommonErrorParser()

    @Test
    fun `getApiErrorResponse by code returns English unknown error message`() {
        val errorId = ErrorId.generate()
        val response = parser.getApiErrorResponse(
            errorId = errorId,
            code = CommonErrorCodes.UNKNOWN,
            args = null,
            locale = LOCALE_EN
        )
        assertEquals(errorId.asHexDashString(), response.id)
        assertEquals(CommonErrorCodes.UNKNOWN, response.code)
        assertEquals(stringsEn.errors.unknown, response.message)
        assertEquals(emptyMap<String, String>(), response.args)
    }

    @Test
    fun `getApiErrorResponse by code returns Russian unknown error message when requested`() {
        val response = parser.getApiErrorResponse(
            errorId = ErrorId.generate(),
            code = CommonErrorCodes.UNKNOWN,
            args = null,
            locale = LOCALE_RU
        )
        assertEquals(stringsRu.errors.unknown, response.message)
    }

    @Test
    fun `getApiErrorResponse falls back to default locale when requested locale missing`() {
        val response = parser.getApiErrorResponse(
            errorId = ErrorId.generate(),
            code = CommonErrorCodes.UNKNOWN,
            args = null,
            locale = "fr"
        )
        assertEquals(stringsEn.errors.unknown, response.message)
    }

    @Test
    fun `getApiErrorResponse from AppError produces fallback message`() {
        val appError = CommonError.Unknown()
        val response = parser.getApiErrorResponse(appError, LOCALE_EN)
        assertEquals(appError.errorId.asHexDashString(), response.id)
        assertEquals(CommonErrorCodes.UNKNOWN, response.code)
        assertEquals(stringsEn.errors.unknown, response.message)
        assertEquals(emptyMap<String, String>(), response.args)
    }

    @Test
    fun `locale is normalized to lowercase`() {
        val response = parser.getApiErrorResponse(
            errorId = ErrorId.generate(),
            code = CommonErrorCodes.UNKNOWN,
            args = null,
            locale = "EN"
        )
        assertEquals(stringsEn.errors.unknown, response.message)
    }

    companion object {
        private const val LOCALE_EN = "en"
        private const val LOCALE_RU = "ru"

        private val stringsEn = CommonModuleStrings(locale = LOCALE_EN)
        private val stringsRu = CommonModuleStrings(locale = LOCALE_RU)
    }
}