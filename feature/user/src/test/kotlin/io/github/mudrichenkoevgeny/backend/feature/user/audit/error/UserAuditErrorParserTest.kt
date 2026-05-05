package io.github.mudrichenkoevgeny.backend.feature.user.audit.error

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.CommonAuditMetadataKey
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.metadata.UserAuditMetadataDeniedReasonValues
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class UserAuditErrorParserTest {

    private val parser = UserAuditErrorParser()

    @ParameterizedTest
    @MethodSource("provideUserErrors")
    fun `parse correctly maps UserError to AuditErrorLogData`(
        error: UserError,
        expectedReason: String
    ) {
        val result = parser.parse(error)

        assertNotNull(result)
        assertEquals(AuditStatus.DENIED, result?.status)

        val metadata = result?.metadata ?: emptySet()

        val idEntry = metadata.find { it.key == CommonAuditMetadataKey.ERROR_ID }
        assertEquals(error.errorId.asHexDashString(), idEntry?.value)

        val reasonEntry = metadata.find { it.key == CommonAuditMetadataKey.DENIED_REASON }
        assertEquals(expectedReason, reasonEntry?.value)
    }

    @Test
    fun `parse returns null for unhandled UserError`() {
        val unhandledErrors = listOf(
            UserError.InvalidAccessToken(),
            UserError.AccessTokenExpired(),
            UserError.InvalidRefreshToken(),
            UserError.InvalidSession(),
            UserError.InvalidCredentials(),
            UserError.CannotDeleteUserIdentifier(),
            UserError.CannotCreateUserIdentifier(),
            UserError.WrongPassword(),
            UserError.WrongConfirmationCode(),
            UserError.ExternalIdentifierLinkageFailed("error")
        )

        unhandledErrors.forEach { error ->
            assertNull(parser.parse(error), "Parser should return null for ${error::class.simpleName}")
        }
    }

    @Test
    fun `parse returns null for CommonError`() {
        val error = CommonError.NotFound(resource = "User", identifier = "123")
        val result = parser.parse(error)
        assertNull(result)
    }

    @Test
    fun `parse returns null for Unknown error`() {
        val error = CommonError.Unknown("test")
        val result = parser.parse(error)
        assertNull(result)
    }

    companion object {
        @JvmStatic
        fun provideUserErrors(): Stream<Arguments> = Stream.of(
            Arguments.of(UserError.UserForbidden(), UserAuditMetadataDeniedReasonValues.USER_FORBIDDEN),
            Arguments.of(UserError.UserReadOnly(), UserAuditMetadataDeniedReasonValues.USER_READ_ONLY),
            Arguments.of(UserError.UserBlocked(), UserAuditMetadataDeniedReasonValues.USER_BLOCKED),
            Arguments.of(UserError.UserSecurityHold(), UserAuditMetadataDeniedReasonValues.USER_SECURITY_HOLD),
            Arguments.of(UserError.UserPendingDeletion(), UserAuditMetadataDeniedReasonValues.USER_PENDING_DELETION),
            Arguments.of(UserError.UserNotFound(), UserAuditMetadataDeniedReasonValues.USER_NOT_FOUND),
            Arguments.of(UserError.UserRoleNotAllowed(), UserAuditMetadataDeniedReasonValues.USER_ROLE_NOT_ALLOWED),
            Arguments.of(UserError.UserMissingPermissions(), UserAuditMetadataDeniedReasonValues.USER_MISSING_PERMISSIONS),
            Arguments.of(
                UserError.UserInsufficientAuthorityLevel(),
                UserAuditMetadataDeniedReasonValues.USER_INSUFFICIENT_AUTHORITY_LEVEL
            ),
            Arguments.of(UserError.UserIllegalAccountStatus(), UserAuditMetadataDeniedReasonValues.USER_ILLEGAL_ACCOUNT_STATUS),
            Arguments.of(
                UserError.UserIdentifierLimitReached(5, UserAuthProvider.EMAIL),
                UserAuditMetadataDeniedReasonValues.USER_IDENTIFIER_LIMIT_REACHED
            ),
            Arguments.of(
                UserError.TotalUserIdentifiersLimitReached(10),
                UserAuditMetadataDeniedReasonValues.TOTAL_USER_IDENTIFIERS_LIMIT_REACHED
            )
        )
    }
}