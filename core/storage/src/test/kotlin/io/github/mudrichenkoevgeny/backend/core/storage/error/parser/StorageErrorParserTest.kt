package io.github.mudrichenkoevgeny.backend.core.storage.error.parser

import io.github.mudrichenkoevgeny.backend.core.common.error.model.ErrorId
import io.github.mudrichenkoevgeny.backend.core.common.error.model.createTestAppError
import io.github.mudrichenkoevgeny.shared.foundation.core.common.formatting.ByteSizeFormatter
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.error.naming.StorageErrorArgs
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.error.naming.StorageErrorCodes
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.localization.StorageModuleStrings
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Unit tests for [StorageErrorParser].
 *
 * Verifies that [StorageErrorParser] resolves storage error codes via [StorageModuleStrings]
 * and handles parameterized error formatting.
 */
class StorageErrorParserTest {

    private val parser = StorageErrorParser()
    private val stringsEn = StorageModuleStrings(locale = "en")
    private val stringsRu = StorageModuleStrings(locale = "ru")

    @Test
    fun `getApiErrorResponse by code returns formatted localized message with arguments`() {
        val errorId = ErrorId.generate()
        val response = parser.getApiErrorResponse(
            errorId = errorId,
            code = StorageErrorCodes.FILE_SIZE_EXCEEDS_LIMIT,
            args = mapOf(StorageErrorArgs.MAX_FILE_SIZE_BYTES to "52428800"),
            locale = "en"
        )
        assertNotNull(response)
        assertEquals(errorId.asHexDashString(), response!!.id)
        assertEquals(StorageErrorCodes.FILE_SIZE_EXCEEDS_LIMIT, response.code)
        val expectedSize = ByteSizeFormatter.format("52428800", locale = "en")!!
        assertEquals(stringsEn.errors.fileSizeExceedsLimit(expectedSize), response.message)
    }

    @Test
    fun `getApiErrorResponse by code returns without args message when args missing`() {
        val errorId = ErrorId.generate()
        val response = parser.getApiErrorResponse(
            errorId = errorId,
            code = StorageErrorCodes.FILE_SIZE_EXCEEDS_LIMIT,
            args = null,
            locale = "en"
        )
        assertNotNull(response)
        assertEquals(stringsEn.errors.fileSizeExceedsLimitWithoutArgs, response!!.message)
    }

    @Test
    fun `getApiErrorResponse by code returns Russian message when requested`() {
        val response = parser.getApiErrorResponse(
            errorId = ErrorId.generate(),
            code = StorageErrorCodes.FILE_SIZE_EXCEEDS_LIMIT,
            args = mapOf(StorageErrorArgs.MAX_FILE_SIZE_BYTES to "52428800"),
            locale = "ru"
        )
        assertNotNull(response)
        val expectedSize = ByteSizeFormatter.format("52428800", locale = "ru")!!
        assertEquals(stringsRu.errors.fileSizeExceedsLimit(expectedSize), response!!.message)
    }

    @Test
    fun `getApiErrorResponse returns null for unhandled error code`() {
        val appError = createTestAppError(code = "UNKNOWN_NON_STORAGE_CODE")

        val response = parser.getApiErrorResponse(appError, "en")

        assertNull(response)
    }
}
