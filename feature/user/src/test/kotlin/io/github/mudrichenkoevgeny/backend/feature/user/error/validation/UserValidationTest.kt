package io.github.mudrichenkoevgeny.backend.feature.user.error.validation

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.user.createTestUserDetails
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.accountlockout.AccountLockoutType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours

class UserValidationTest {

    private val defaultAllowedRoles = setOf(UserRole.USER, UserRole.ADMIN)
    private val defaultAllowedStatuses = setOf(UserAccountStatus.ACTIVE, UserAccountStatus.READ_ONLY)

    @Test
    fun `validateRoleAndStatus returns Success when role, status, and lockout are valid`() {
        val user = createTestUserDetails(
            role = UserRole.USER,
            accountStatus = UserAccountStatus.ACTIVE,
            lockoutType = AccountLockoutType.NONE
        )

        val result = user.validateAccessEligibility(defaultAllowedRoles, defaultAllowedStatuses)

        assertTrue(result is AppResult.Success)
    }

    @Test
    fun `validateRoleAndStatus returns Error UserRoleNotAllowed when role is not in allowedRoles`() {
        val user = createTestUserDetails(role = UserRole.STAFF)

        val result = user.validateAccessEligibility(setOf(UserRole.USER), defaultAllowedStatuses)

        assertTrue(result is AppResult.Error)
        val error = (result as AppResult.Error).error
        assertTrue(error is UserError.UserRoleNotAllowed)
        assertEquals(user.id, (error as UserError.UserRoleNotAllowed).userId)
    }

    @Test
    fun `validateRoleAndStatus returns Error UserReadOnly when status is READ_ONLY and not allowed`() {
        val user = createTestUserDetails(accountStatus = UserAccountStatus.READ_ONLY)

        val result = user.validateAccessEligibility(defaultAllowedRoles, setOf(UserAccountStatus.ACTIVE))

        assertTrue(result is AppResult.Error)
        val error = (result as AppResult.Error).error
        assertTrue(error is UserError.UserReadOnly)
        assertEquals(user.id, (error as UserError.UserReadOnly).userId)
    }

    @Test
    fun `validateRoleAndStatus returns Error UserBanned when status is BANNED`() {
        val user = createTestUserDetails(accountStatus = UserAccountStatus.BANNED)

        val result = user.validateAccessEligibility(defaultAllowedRoles, defaultAllowedStatuses)

        assertTrue(result is AppResult.Error)
        val error = (result as AppResult.Error).error
        assertTrue(error is UserError.UserBanned)
        assertEquals(user.id, (error as UserError.UserBanned).userId)
    }

    @Test
    fun `validateRoleAndStatus returns Error UserSecurityHold when status is SECURITY_HOLD`() {
        val user = createTestUserDetails(accountStatus = UserAccountStatus.SECURITY_HOLD)

        val result = user.validateAccessEligibility(defaultAllowedRoles, defaultAllowedStatuses)

        assertTrue(result is AppResult.Error)
        val error = (result as AppResult.Error).error
        assertTrue(error is UserError.UserSecurityHold)
        assertEquals(user.id, (error as UserError.UserSecurityHold).userId)
    }

    @Test
    fun `validateRoleAndStatus returns Error UserPendingDeletion when status is PENDING_DELETION`() {
        val user = createTestUserDetails(accountStatus = UserAccountStatus.PENDING_DELETION)

        val result = user.validateAccessEligibility(defaultAllowedRoles, defaultAllowedStatuses)

        assertTrue(result is AppResult.Error)
        val error = (result as AppResult.Error).error
        assertTrue(error is UserError.UserPendingDeletion)
        assertEquals(user.id, (error as UserError.UserPendingDeletion).userId)
    }

    @Test
    fun `validateRoleAndStatus returns Error UserLocked when lockoutType is INDEFINITE`() {
        val user = createTestUserDetails(
            role = UserRole.USER,
            accountStatus = UserAccountStatus.ACTIVE,
            lockoutType = AccountLockoutType.INDEFINITE
        )

        val result = user.validateAccessEligibility(defaultAllowedRoles, defaultAllowedStatuses)

        assertTrue(result is AppResult.Error)
        val error = (result as AppResult.Error).error
        assertTrue(error is UserError.UserLocked)
        assertEquals(AccountLockoutType.INDEFINITE, (error as UserError.UserLocked).lockoutType)
    }

    @Test
    fun `validateRoleAndStatus returns Error UserLocked when lockoutType is TEMPORARY and time is in future`() {
        val futureTime = Clock.System.now() + 1.hours
        val user = createTestUserDetails(
            role = UserRole.USER,
            accountStatus = UserAccountStatus.ACTIVE,
            lockoutType = AccountLockoutType.TEMPORARY,
            temporaryLockoutUntil = futureTime
        )

        val result = user.validateAccessEligibility(defaultAllowedRoles, defaultAllowedStatuses)

        assertTrue(result is AppResult.Error)
        val error = (result as AppResult.Error).error
        assertTrue(error is UserError.UserLocked)
        val userLocked = error as UserError.UserLocked
        assertEquals(AccountLockoutType.TEMPORARY, userLocked.lockoutType)
        assertEquals(futureTime, userLocked.temporaryLockoutUntil)
    }

    @Test
    fun `validateRoleAndStatus returns Error UserLocked when lockoutType is TEMPORARY and time is null`() {
        val user = createTestUserDetails(
            role = UserRole.USER,
            accountStatus = UserAccountStatus.ACTIVE,
            lockoutType = AccountLockoutType.TEMPORARY,
            temporaryLockoutUntil = null
        )

        val result = user.validateAccessEligibility(defaultAllowedRoles, defaultAllowedStatuses)

        assertTrue(result is AppResult.Error)
        val error = (result as AppResult.Error).error
        assertTrue(error is UserError.UserLocked)
        assertEquals(AccountLockoutType.TEMPORARY, (error as UserError.UserLocked).lockoutType)
    }

    @Test
    fun `validateRoleAndStatus returns Success when lockoutType is TEMPORARY but time is in past`() {
        val pastTime = Clock.System.now() - 1.hours
        val user = createTestUserDetails(
            role = UserRole.USER,
            accountStatus = UserAccountStatus.ACTIVE,
            lockoutType = AccountLockoutType.TEMPORARY,
            temporaryLockoutUntil = pastTime
        )

        val result = user.validateAccessEligibility(defaultAllowedRoles, defaultAllowedStatuses)

        assertTrue(result is AppResult.Success)
    }
}
