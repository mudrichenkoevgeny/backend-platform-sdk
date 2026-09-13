package io.github.mudrichenkoevgeny.backend.core.common.error.parser

import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppErrorParserConfig
import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.error.model.ErrorId
import io.github.mudrichenkoevgeny.shared.foundation.core.common.error.naming.CommonErrorArgs
import io.github.mudrichenkoevgeny.shared.foundation.core.common.error.naming.CommonErrorCodes
import io.github.mudrichenkoevgeny.shared.foundation.core.common.serialization.FoundationJson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Unit tests for [CommonErrorParser].
 *
 * Verifies error message loading from JSON localization files (`localization/en` and `localization/ru`).
 * Ensures template placeholders are substituted correctly with provided arguments.
 * Validates locale normalization and fallback behavior.
 * Confirms default responses when codes or resources are missing.
 *
 * Expected messages are dynamically loaded from JSON files rather than hardcoded in assertions.
 */
class CommonErrorParserTest {

    private val parser = CommonErrorParser(
        AppErrorParserConfig(
            resourcePaths = listOf(LOCALIZATION_RESOURCE_PATH),
            supportedLocales = setOf(LOCALE_EN, LOCALE_RU),
        )
    )

    /**
     * Verifies that [CommonErrorParser.getApiErrorResponse] resolves the English message for a known error code without arguments.
     */
    @Test
    fun `getApiErrorResponse by code returns English message when no args`() {
        val errorId = ErrorId.generate()
        val response = parser.getApiErrorResponse(
            errorId = errorId,
            code = CommonErrorCodes.UNKNOWN,
            args = null,
            locale = LOCALE_EN
        )
        assertEquals(errorId.asHexDashString(), response.id)
        assertEquals(CommonErrorCodes.UNKNOWN, response.code)
        assertEquals(messagesEn[CommonErrorCodes.UNKNOWN], response.message)
        assertEquals(emptyMap<String, String>(), response.args)
    }

    /**
     * Verifies that [CommonErrorParser.getApiErrorResponse] resolves the Russian localized message when requested.
     */
    @Test
    fun `getApiErrorResponse by code returns Russian message when no args`() {
        val response = parser.getApiErrorResponse(
            errorId = ErrorId.generate(),
            code = CommonErrorCodes.UNKNOWN,
            args = null,
            locale = LOCALE_RU
        )
        assertEquals(messagesRu[CommonErrorCodes.UNKNOWN], response.message)
    }

    /**
     * Verifies that placeholders (e.g. `{fieldName}`) in English message templates are substituted with provided arguments.
     */
    @Test
    fun `getApiErrorResponse replaces placeholder with arg value`() {
        val template = messagesEn[CommonErrorCodes.MISSING_REQUIRED_FIELD]!!
        val expectedMessage = template.replace("{$PLACEHOLDER_FIELD_NAME}", TEST_FIELD_VALUE)
        val response = parser.getApiErrorResponse(
            errorId = ErrorId.generate(),
            code = CommonErrorCodes.MISSING_REQUIRED_FIELD,
            args = mapOf(CommonErrorArgs.FIELD_NAME to TEST_FIELD_VALUE),
            locale = LOCALE_EN
        )
        assertEquals(expectedMessage, response.message)
        assertEquals(mapOf(CommonErrorArgs.FIELD_NAME to TEST_FIELD_VALUE), response.args)
    }

    /**
     * Verifies that placeholders in Russian message templates are substituted with provided arguments.
     */
    @Test
    fun `getApiErrorResponse replaces placeholder for Russian locale`() {
        val template = messagesRu[CommonErrorCodes.MISSING_REQUIRED_FIELD]!!
        val expectedMessage = template.replace("{$PLACEHOLDER_FIELD_NAME}", TEST_FIELD_VALUE)
        val response = parser.getApiErrorResponse(
            errorId = ErrorId.generate(),
            code = CommonErrorCodes.MISSING_REQUIRED_FIELD,
            args = mapOf(CommonErrorArgs.FIELD_NAME to TEST_FIELD_VALUE),
            locale = LOCALE_RU
        )
        assertEquals(expectedMessage, response.message)
    }

    /**
     * Verifies that [CommonErrorParser.getApiErrorResponse] falls back to the default locale when an unsupported locale is requested.
     */
    @Test
    fun `getApiErrorResponse falls back to default locale when requested locale missing`() {
        val response = parser.getApiErrorResponse(
            errorId = ErrorId.generate(),
            code = CommonErrorCodes.UNKNOWN,
            args = null,
            locale = "fr"
        )
        assertEquals(messagesEn[CommonErrorCodes.UNKNOWN], response.message)
    }

    /**
     * Verifies that [CommonErrorParser.getApiErrorResponse] returns [UNKNOWN_ERROR_MESSAGE] when an error code is not present in localization files.
     */
    @Test
    fun `getApiErrorResponse returns UNKNOWN_ERROR_MESSAGE when code not in localization`() {
        val response = parser.getApiErrorResponse(
            errorId = ErrorId.generate(),
            code = NONEXISTENT_ERROR_CODE,
            args = null,
            locale = LOCALE_EN
        )
        assertEquals(UNKNOWN_ERROR_MESSAGE, response.message)
    }

    /**
     * Verifies that passing an [AppError] instance extracts its error ID, code, and public arguments to build the response message.
     */
    @Test
    fun `getApiErrorResponse from AppError uses code publicArgs and errorId`() {
        val appError = CommonError.MissingRequiredField(TEST_FIELD_VALUE)
        val template = messagesEn[CommonErrorCodes.MISSING_REQUIRED_FIELD]!!
        val expectedMessage = template.replace("{$PLACEHOLDER_FIELD_NAME}", TEST_FIELD_VALUE)
        val response = parser.getApiErrorResponse(appError, LOCALE_EN)
        assertEquals(appError.errorId.asHexDashString(), response.id)
        assertEquals(appError.code, response.code)
        assertEquals(expectedMessage, response.message)
        assertEquals(mapOf(CommonErrorArgs.FIELD_NAME to TEST_FIELD_VALUE), response.args)
    }

    /**
     * Verifies that an [AppError] without public arguments produces a static localized message with empty arguments in response.
     */
    @Test
    fun `getApiErrorResponse from AppError without publicArgs returns static message`() {
        val appError = CommonError.Unknown()
        val response = parser.getApiErrorResponse(appError, LOCALE_EN)
        assertEquals(appError.errorId.asHexDashString(), response.id)
        assertEquals(CommonErrorCodes.UNKNOWN, response.code)
        assertEquals(messagesEn[CommonErrorCodes.UNKNOWN], response.message)
        assertEquals(emptyMap<String, String>(), response.args)
    }

    /**
     * Verifies that when localization resources fail to load, [CommonErrorParser] safely returns [UNKNOWN_ERROR_MESSAGE].
     */
    @Test
    fun `when no resources loaded returns UNKNOWN_ERROR_MESSAGE`() {
        val emptyParser = CommonErrorParser(
            AppErrorParserConfig(
                resourcePaths = listOf("nonexistent_path"),
                supportedLocales = setOf(LOCALE_EN)
            )
        )
        val response = emptyParser.getApiErrorResponse(
            errorId = ErrorId.generate(),
            code = CommonErrorCodes.UNKNOWN,
            args = null,
            locale = LOCALE_EN
        )
        assertEquals(UNKNOWN_ERROR_MESSAGE, response.message)
    }

    /**
     * Verifies that locale strings containing uppercase characters (e.g., `"EN"`) are normalized to lowercase (`"en"`).
     */
    @Test
    fun `locale is normalized to lowercase`() {
        val response = parser.getApiErrorResponse(
            errorId = ErrorId.generate(),
            code = CommonErrorCodes.UNKNOWN,
            args = null,
            locale = "EN"
        )
        assertEquals(messagesEn[CommonErrorCodes.UNKNOWN], response.message)
    }

    companion object {
        private const val LOCALIZATION_RESOURCE_PATH = "localization"
        private const val LOCALE_EN = "en"
        private const val LOCALE_RU = "ru"
        private const val TEST_FIELD_VALUE = "email"
        private const val PLACEHOLDER_FIELD_NAME = "fieldName"
        private const val NONEXISTENT_ERROR_CODE = "NON_EXISTENT_CODE"

        private val messagesEn: Map<String, String> = loadTestMessages(LOCALE_EN)
        private val messagesRu: Map<String, String> = loadTestMessages(LOCALE_RU)

        private fun loadTestMessages(locale: String): Map<String, String> {
            val resourceName = "$LOCALIZATION_RESOURCE_PATH/$locale/error_messages.json"
            val stream = CommonErrorParserTest::class.java.classLoader.getResourceAsStream(resourceName)
                ?: return emptyMap()
            return stream.use {
                val text = it.bufferedReader().readText().trim()
                if (text.isEmpty() || text == "{}" || text == "null") emptyMap()
                else FoundationJson.decodeFromString(text)
            }
        }
    }
}
