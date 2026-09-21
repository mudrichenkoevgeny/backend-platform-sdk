package io.github.mudrichenkoevgeny.backend.feature.user.error.util

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.accountlockout.AccountLockoutType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class UserErrorUtilsTest {

    @Test
    fun `extractUserIdOrNull extracts userId from UserError variants containing userId`() {
        val userId = UserId.generate()

        val errorsWithUserId = listOf(
            UserError.UserBanned(userId = userId),
            UserError.UserLocked(userId = userId, lockoutType = AccountLockoutType.INDEFINITE),
            UserError.UserReadOnly(userId = userId),
            UserError.UserSecurityHold(userId = userId),
            UserError.UserPendingDeletion(userId = userId),
            UserError.UserForbidden(userId = userId),
            UserError.UserRoleNotAllowed(userId = userId),
            UserError.UserMissingPermissions(userId = userId),
            UserError.UserIllegalAccountStatus(userId = userId),
            UserError.UserInsufficientAuthorityLevel(userId = userId),
            UserError.UserNotFound(userId = userId),
            UserError.PasswordSetupRequired(userId = userId)
        )

        errorsWithUserId.forEach { error ->
            assertEquals(userId, error.extractUserIdOrNull(), "Should extract userId for ${error::class.simpleName}")
            assertEquals(userId.asHexDashString(), error.extractUserIdHexOrNull(), "Should extract userId hex for ${error::class.simpleName}")
        }
    }

    @Test
    fun `extractUserIdOrNull returns null for UserError variants without userId`() {
        val errorsWithoutUserId = listOf(
            UserError.InvalidAccessToken(),
            UserError.AccessTokenExpired(),
            UserError.InvalidRefreshToken(),
            UserError.InvalidSession(),
            UserError.InvalidCredentials(),
            UserError.CannotDeleteUserIdentifier(),
            UserError.CannotCreateUserIdentifier(),
            UserError.UserIdentifierLimitReached(5, UserAuthProvider.EMAIL),
            UserError.TotalUserIdentifiersLimitReached(10),
            UserError.WrongPassword(),
            UserError.WrongConfirmationCode(),
            UserError.ExternalIdentifierLinkageFailed("error"),
            UserError.RegistrationDisabled(),
            UserError.SelfServiceUnlockDisabled(),
            UserError.EmailNotAllowed(),
            UserError.UserIdentifierPasswordNotSupported(),
            UserError.UserIdentifierPasswordNotSet()
        )

        errorsWithoutUserId.forEach { error ->
            assertNull(error.extractUserIdOrNull(), "Should return null for ${error::class.simpleName}")
            assertNull(error.extractUserIdHexOrNull(), "Should return null hex for ${error::class.simpleName}")
        }
    }

    @Test
    fun `extractUserIdOrNull returns null for CommonError`() {
        val error = CommonError.NotFound(resource = "User", identifier = "123")
        assertNull(error.extractUserIdOrNull())
        assertNull(error.extractUserIdHexOrNull())
    }
}
