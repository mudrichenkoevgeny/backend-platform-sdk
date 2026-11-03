package io.github.mudrichenkoevgeny.backend.core.audit.error

import io.github.mudrichenkoevgeny.backend.core.audit.domain.model.AuditErrorLogData
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.error.model.createTestAppError
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.CommonAuditMetadataKey
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

/**
 * Unit tests for [CommonAuditErrorParser].
 *
 * Verifies that [CommonAuditErrorParser] maps any [AppError] to an [AuditErrorLogData] instance.
 * The resulting instance has status [AuditStatus.FAILED] and includes error ID and code in metadata.
 */
class CommonAuditErrorParserTest {

    private val parser = CommonAuditErrorParser()

    @Test
    fun `parse maps AppError to AuditErrorLogData with FAILED status and error metadata`() {
        val appError = createTestAppError(code = "CUSTOM_ERROR_CODE")

        val result = parser.parse(appError)

        assertNotNull(result)

        val idMeta = result.metadata.find { it.key == CommonAuditMetadataKey.ERROR_ID }
        val idMetaValue = checkNotNull(idMeta).value
        assertEquals(appError.errorId.asHexDashString(), idMetaValue)

        val codeMeta = result.metadata.find { it.key == CommonAuditMetadataKey.ERROR_CODE }
        val codeMetaValue = checkNotNull(codeMeta).value
        assertEquals("CUSTOM_ERROR_CODE", codeMetaValue)
    }
}
