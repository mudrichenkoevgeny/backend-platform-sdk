package io.github.mudrichenkoevgeny.backend.core.security.audit.error

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.security.error.model.SecurityError
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.CommonAuditMetadataKey
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.audit.metadata.SecurityAuditMetadataDeniedReasonValues
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class SecurityAuditErrorParserTest {

    private val parser = SecurityAuditErrorParser()

    @Test
    fun `parse returns RATE_LIMIT reason for CommonError TooManyRequests`() {
        val error = CommonError.TooManyRequests(
            rateLimitActionCode = "test_action",
            limit = 5,
            identifier = "user_123",
            retryAfterSeconds = 60
        )

        val result = parser.parse(error)

        assertNotNull(result)
        assertEquals(AuditStatus.DENIED, result?.status)
        assertMetadataContains(result?.metadata, CommonAuditMetadataKey.DENIED_REASON, SecurityAuditMetadataDeniedReasonValues.RATE_LIMIT)
        assertMetadataContains(result?.metadata, CommonAuditMetadataKey.ERROR_ID, error.errorId.asHexDashString())
    }

    @Test
    fun `parse returns RATE_LIMIT reason for SecurityError OtpRetryTooSoon`() {
        val error = SecurityError.OtpRetryTooSoon(retryAfterSeconds = 30)

        val result = parser.parse(error)

        assertMetadataContains(result?.metadata, CommonAuditMetadataKey.DENIED_REASON, SecurityAuditMetadataDeniedReasonValues.RATE_LIMIT)
    }

    @Test
    fun `parse returns PASSWORD_TOO_WEAK reason for SecurityError PasswordTooWeak`() {
        val error = SecurityError.PasswordTooWeak(publicArgs = mapOf("min_length" to 8))

        val result = parser.parse(error)

        assertMetadataContains(result?.metadata, CommonAuditMetadataKey.DENIED_REASON, SecurityAuditMetadataDeniedReasonValues.PASSWORD_TOO_WEAK)
    }

    @Test
    fun `parse returns MFA_TOKEN_EXPIRED reason for SecurityError MfaTokenExpired`() {
        val error = SecurityError.MfaTokenExpired()

        val result = parser.parse(error)

        assertMetadataContains(result?.metadata, CommonAuditMetadataKey.DENIED_REASON, SecurityAuditMetadataDeniedReasonValues.MFA_TOKEN_EXPIRED)
    }

    @Test
    fun `parse returns TOTP_ALREADY_ENABLED reason for SecurityError TotpAlreadyEnabled`() {
        val error = SecurityError.TotpAlreadyEnabled()

        val result = parser.parse(error)

        assertMetadataContains(result?.metadata, CommonAuditMetadataKey.DENIED_REASON, SecurityAuditMetadataDeniedReasonValues.TOTP_ALREADY_ENABLED)
    }

    @Test
    fun `parse returns TOTP_NOT_ENABLED reason for SecurityError TotpNotEnabled`() {
        val error = SecurityError.TotpNotEnabled()

        val result = parser.parse(error)

        assertMetadataContains(result?.metadata, CommonAuditMetadataKey.DENIED_REASON, SecurityAuditMetadataDeniedReasonValues.TOTP_NOT_ENABLED)
    }

    @Test
    fun `parse returns RECOVERY_CODE_ALREADY_USED reason for SecurityError RecoveryCodeAlreadyUsed`() {
        val error = SecurityError.RecoveryCodeAlreadyUsed()

        val result = parser.parse(error)

        assertMetadataContains(result?.metadata, CommonAuditMetadataKey.DENIED_REASON, SecurityAuditMetadataDeniedReasonValues.RECOVERY_CODE_ALREADY_USED)
    }

    @Test
    fun `parse returns MFA_REQUIRED reason for SecurityError TotpConfirmationRequired`() {
        val error = SecurityError.TotpConfirmationRequired(mfaToken = "test-mfa-token")

        val result = parser.parse(error)

        assertMetadataContains(result?.metadata, CommonAuditMetadataKey.DENIED_REASON, SecurityAuditMetadataDeniedReasonValues.MFA_REQUIRED)
    }

    @Test
    fun `parse returns null for unhandled CommonError`() {
        val error = CommonError.NotFound(resource = "User", identifier = "123")

        val result = parser.parse(error)

        assertNull(result)
    }

    @Test
    fun `parse returns null for unhandled SecurityError`() {
        val error = SecurityError.InvalidTotpCode()

        val result = parser.parse(error)

        assertNull(result)
    }

    private fun assertMetadataContains(
        metadata: Set<io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.AuditEventMetadata>?,
        key: io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.AuditMetadataKey,
        value: String
    ) {
        val entry = metadata?.find { it.key == key }
        assertNotNull(entry, "Metadata key ${key.serialName} not found")
        assertEquals(value, entry?.value)
    }
}