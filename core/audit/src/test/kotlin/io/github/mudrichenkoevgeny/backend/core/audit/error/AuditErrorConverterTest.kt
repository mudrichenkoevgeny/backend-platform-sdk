package io.github.mudrichenkoevgeny.backend.core.audit.error

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.error.model.createTestAppError
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AuditErrorConverterTest {

    private val stubParser = TestAuditErrorParser(
        targetErrorCode = "special_error",
        resultStatus = AuditStatus.DENIED
    )

    private val commonParser = CommonAuditErrorParser()
    private val converter = AuditErrorConverter(
        parsers = setOf(stubParser),
        commonParser = commonParser
    )

    @Test
    fun `convert uses specialized parser when match found`() {
        val error = createTestAppError(code = "special_error")

        val result = converter.convert(error)

        assertEquals(AuditStatus.DENIED, result.status)
    }

    @Test
    fun `convert falls back to common parser when no specialized parser matches`() {
        val error = CommonError.Unknown()

        val result = converter.convert(error)

        assertEquals(AuditStatus.FAILED, result.status)
        assertTrue(result.metadata.any { it.value == error.code })
    }
}
