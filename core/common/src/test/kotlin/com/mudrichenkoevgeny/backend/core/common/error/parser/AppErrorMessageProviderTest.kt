/*
package io.github.mudrichenkoevgeny.backend.core.common.error.parser

import io.github.mudrichenkoevgeny.backend.core.common.error.constants.CommonErrorArgs
import io.github.mudrichenkoevgeny.backend.core.common.error.constants.CommonErrorCodes
import io.github.mudrichenkoevgeny.backend.core.common.error.model.ErrorId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

private const val UNKNOWN_ERROR_EN = "An unknown error has occurred." // from app_errors/error_messages_en.json
private const val UNKNOWN_ERROR_RU = "Произошла неизвестная ошибка." // from app_errors/error_messages_ru.json

class AppErrorMessageProviderTest {

    private lateinit var appErrorParser: AppErrorParser

    private val errorId = ErrorId(UUID.randomUUID())

    @BeforeEach
    fun setUp() {
        appErrorParser = AppErrorParserImpl()
        appErrorParser.configure(
            resourcePaths = listOf("app_errors"),
            supportedLocales = setOf("en", "ru")
        )
    }

    @Test
    fun `should return English message without args`() {
        // GIVEN
        val code = CommonErrorCodes.UNKNOWN
        val locale = "en"
        // WHEN
        val apiError = appErrorParser.getApiError(
            errorId = errorId,
            code = code,
            locale = locale
        )
        // THEN
        assertEquals(UNKNOWN_ERROR_EN, apiError.message)
    }

    @Test
    fun `should return Russian message without args`() {
        // GIVEN
        val code = CommonErrorCodes.UNKNOWN
        val locale = "ru"
        // WHEN
        val apiError = appErrorParser.getApiError(
            errorId = errorId,
            code = code,
            locale = locale
        )
        // THEN
        assertEquals(UNKNOWN_ERROR_RU, apiError.message)
    }

    @Test
    fun `should replace argument in template`() {
        // GIVEN
        val code = CommonErrorCodes.MISSING_REQUIRED_FIELD
        val locale = "en"
        val args = mapOf(CommonErrorArgs.FIELD_NAME to "email")
        // WHEN
        val apiError = appErrorParser.getApiError(
            errorId = errorId,
            code = code,
            args = args,
            locale = locale
        )
        // THEN
        assertEquals("The required field 'email' is missing.", apiError.message)
    }

    @Test
    fun `should fallback to default locale if locale missing`() {
        // GIVEN
        val code = CommonErrorCodes.UNKNOWN
        val missingLocale = "fr"
        // WHEN
        val apiError = appErrorParser.getApiError(
            errorId = errorId,
            code = code,
            locale = missingLocale
        )
        // THEN
        assertEquals(UNKNOWN_ERROR_EN, apiError.message)
    }

    @Test
    fun `should fallback to default locale for missing code`() {
        // GIVEN
        val missingCode = "NON_EXISTENT_CODE"
        val locale = "en"
        // WHEN
        val apiError = appErrorParser.getApiError(
            errorId = errorId,
            code = missingCode,
            locale = locale
        )
        // THEN
        assertEquals(UNKNOWN_ERROR_MESSAGE, apiError.message)
    }
}*/
