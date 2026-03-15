package io.github.mudrichenkoevgeny.backend.core.common.error.parser

import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppErrorParserConfig
import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.error.model.ErrorId
import io.github.mudrichenkoevgeny.shared.foundation.core.common.error.naming.CommonErrorArgs
import io.github.mudrichenkoevgeny.shared.foundation.core.common.error.naming.CommonErrorCodes
import io.github.mudrichenkoevgeny.shared.foundation.core.common.serialization.FoundationJson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Tests for [AppErrorParserImpl] using test resources under `localization/en` and `localization/ru`.
 * Expected messages are loaded from the same JSON files the parser uses, so no message strings are hardcoded.
 */
class AppErrorParserImplTest {

    private val parser = AppErrorParserImpl(
        AppErrorParserConfig(
            resourcePaths = listOf(LOCALIZATION_RESOURCE_PATH),
            supportedLocales = setOf(LOCALE_EN, LOCALE_RU)
        )
    )

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

    @Test
    fun `getApiErrorResponse from AppError without publicArgs returns static message`() {
        val appError = CommonError.Unknown()
        val response = parser.getApiErrorResponse(appError, LOCALE_EN)
        assertEquals(appError.errorId.asHexDashString(), response.id)
        assertEquals(CommonErrorCodes.UNKNOWN, response.code)
        assertEquals(messagesEn[CommonErrorCodes.UNKNOWN], response.message)
        assertEquals(emptyMap<String, String>(), response.args)
    }

    @Test
    fun `when no resources loaded returns UNKNOWN_ERROR_MESSAGE`() {
        val emptyParser = AppErrorParserImpl(
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
            val stream = AppErrorParserImplTest::class.java.classLoader.getResourceAsStream(resourceName)
                ?: return emptyMap()
            return stream.use {
                val text = it.bufferedReader().readText().trim()
                if (text.isEmpty() || text == "{}" || text == "null") emptyMap()
                else FoundationJson.decodeFromString(text)
            }
        }
    }
}
