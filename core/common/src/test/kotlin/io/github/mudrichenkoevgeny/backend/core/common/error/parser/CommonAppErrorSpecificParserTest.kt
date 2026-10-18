package io.github.mudrichenkoevgeny.backend.core.common.error.parser

import io.github.mudrichenkoevgeny.backend.core.common.error.model.ErrorId
import io.github.mudrichenkoevgeny.backend.core.common.error.model.createTestAppError
import io.github.mudrichenkoevgeny.shared.foundation.core.common.error.naming.CommonErrorArgs
import io.github.mudrichenkoevgeny.shared.foundation.core.common.error.naming.CommonErrorCodes
import io.github.mudrichenkoevgeny.shared.foundation.core.common.localization.CommonModuleStrings
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CommonAppErrorSpecificParserTest {

    private val parser = CommonAppErrorSpecificParser()
    private val stringsEn = CommonModuleStrings(locale = "en")
    private val stringsRu = CommonModuleStrings(locale = "ru")

    @Test
    fun `getApiErrorResponse by code returns localized message for MISSING_REQUIRED_FIELD`() {
        val errorId = ErrorId.generate()
        val response = parser.getApiErrorResponse(
            errorId = errorId,
            code = CommonErrorCodes.MISSING_REQUIRED_FIELD,
            args = mapOf(CommonErrorArgs.FIELD_NAME to "email"),
            locale = "en"
        )
        assertNotNull(response)
        assertEquals(errorId.asHexDashString(), response!!.id)
        assertEquals(CommonErrorCodes.MISSING_REQUIRED_FIELD, response.code)
        assertEquals(stringsEn.errors.missingField("email"), response.message)
    }

    @Test
    fun `getApiErrorResponse by code returns Russian message when requested`() {
        val response = parser.getApiErrorResponse(
            errorId = ErrorId.generate(),
            code = CommonErrorCodes.MISSING_REQUIRED_FIELD,
            args = mapOf(CommonErrorArgs.FIELD_NAME to "email"),
            locale = "ru"
        )
        assertNotNull(response)
        assertEquals(stringsRu.errors.missingField("email"), response!!.message)
    }

    @Test
    fun `getApiErrorResponse returns null for unhandled error code`() {
        val appError = createTestAppError(code = "CUSTOM_FEATURE_ERROR_CODE")

        val response = parser.getApiErrorResponse(appError, "en")

        assertNull(response)
    }
}