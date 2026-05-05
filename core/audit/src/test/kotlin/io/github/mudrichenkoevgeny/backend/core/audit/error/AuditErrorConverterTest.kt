package io.github.mudrichenkoevgeny.backend.core.audit.error

import io.github.mudrichenkoevgeny.backend.core.audit.domain.model.AuditErrorLogData
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppErrorSeverity
import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.error.model.ErrorId
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.ktor.http.HttpStatusCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AuditErrorConverterTest {

    private val stubParser = object : AuditErrorParser {
        override fun parse(error: AppError): AuditErrorLogData? {
            return if (error.code == "special_error") {
                AuditErrorLogData(AuditStatus.DENIED, emptySet())
            } else null
        }
    }

    private val commonParser = CommonAuditErrorParser()
    private val converter = AuditErrorConverter(
        parsers = setOf(stubParser),
        commonParser = commonParser
    )

    @Test
    fun `convert uses specialized parser when match found`() {
        val error = createTestError("special_error")

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

    private fun createTestError(errorCode: String) = object : AppError {
        override val errorId = ErrorId.generate()
        override val code = errorCode
        override val publicArgs = null
        override val secretArgs = null
        override val httpStatusCode = HttpStatusCode.InternalServerError
        override val appErrorSeverity = AppErrorSeverity.MEDIUM
    }
}