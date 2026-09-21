package io.github.mudrichenkoevgeny.backend.feature.user.manager.lockout

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.lockout.LockoutAttemptType
import io.github.mudrichenkoevgeny.backend.core.security.lockout.LockoutManager
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.accountlockout.AccountLockoutType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

class UserLockoutServiceImplTest {

    private val lockoutManager = mockk<LockoutManager>()
    private val userManager = mockk<UserManager>()

    private val service = UserLockoutServiceImpl(
        lockoutManager = lockoutManager,
        userManager = userManager
    )

    private val userId = UserId.generate()
    private val testIdentifier = "test@example.com"
    private val secondIdentifier = "userId-1234"

    @Test
    fun `checkLockout with single identifier returns Success when not locked out`() = runTest {
        coEvery { lockoutManager.isIndefiniteLockout(testIdentifier) } returns AppResult.Success(false)
        coEvery { lockoutManager.getLockoutUntil(testIdentifier) } returns AppResult.Success(null)

        val result = service.checkLockout(testIdentifier, userId)

        assertTrue(result is AppResult.Success)
        coVerify(exactly = 0) { userManager.lockUserAccount(any(), any()) }
        coVerify(exactly = 0) { userManager.lockUserAccountIndefinitely(any()) }
    }

    @Test
    fun `checkLockout returns Error UserLocked and locks user indefinitely when indefinite lockout is active`() = runTest {
        coEvery { lockoutManager.isIndefiniteLockout(testIdentifier) } returns AppResult.Success(true)
        coEvery { userManager.lockUserAccountIndefinitely(userId) } returns AppResult.Success(mockk())

        val result = service.checkLockout(listOf(testIdentifier, secondIdentifier), userId)

        assertTrue(result is AppResult.Error)
        val error = (result as AppResult.Error).error
        assertTrue(error is UserError.UserLocked)
        val userLocked = error as UserError.UserLocked
        assertEquals(userId, userLocked.userId)
        assertEquals(AccountLockoutType.INDEFINITE, userLocked.lockoutType)

        coVerify(exactly = 1) { userManager.lockUserAccountIndefinitely(userId) }
    }

    @Test
    fun `checkLockout returns Error UserLocked without locking user in DB when indefinite lockout is active and userId is null`() = runTest {
        coEvery { lockoutManager.isIndefiniteLockout(testIdentifier) } returns AppResult.Success(true)

        val result = service.checkLockout(testIdentifier, userId = null)

        assertTrue(result is AppResult.Error)
        val error = (result as AppResult.Error).error
        assertTrue(error is UserError.UserLocked)
        val userLocked = error as UserError.UserLocked
        assertEquals(null, userLocked.userId)
        assertEquals(AccountLockoutType.INDEFINITE, userLocked.lockoutType)

        coVerify(exactly = 0) { userManager.lockUserAccountIndefinitely(any()) }
    }

    @Test
    fun `checkLockout returns Error UserLocked with max lockout time when temporary lockout is active`() = runTest {
        val now = Clock.System.now()
        val lockout1 = now + 5.minutes
        val lockout2 = now + 15.minutes

        coEvery { lockoutManager.isIndefiniteLockout(any()) } returns AppResult.Success(false)
        coEvery { lockoutManager.getLockoutUntil(testIdentifier) } returns AppResult.Success(lockout1)
        coEvery { lockoutManager.getLockoutUntil(secondIdentifier) } returns AppResult.Success(lockout2)

        val result = service.checkLockout(listOf(testIdentifier, secondIdentifier), userId)

        assertTrue(result is AppResult.Error)
        val error = (result as AppResult.Error).error
        assertTrue(error is UserError.UserLocked)
        val userLocked = error as UserError.UserLocked
        assertEquals(userId, userLocked.userId)
        assertEquals(AccountLockoutType.TEMPORARY, userLocked.lockoutType)
        assertEquals(lockout2, userLocked.temporaryLockoutUntil)
    }

    @Test
    fun `recordFailedAttempt with single identifier returns Success when threshold not reached`() = runTest {
        coEvery {
            lockoutManager.recordFailedAttempt(testIdentifier, LockoutAttemptType.PASSWORD)
        } returns AppResult.Success(null)

        val result = service.recordFailedAttempt(testIdentifier, LockoutAttemptType.PASSWORD, userId)

        assertTrue(result is AppResult.Success)
        coVerify(exactly = 0) { userManager.lockUserAccount(any(), any()) }
    }

    @Test
    fun `recordFailedAttempt returns Error UserLocked and locks user in DB when lockout threshold is exceeded`() = runTest {
        val lockoutTime = Clock.System.now() + 10.minutes

        coEvery {
            lockoutManager.recordFailedAttempt(testIdentifier, LockoutAttemptType.OTP)
        } returns AppResult.Success(lockoutTime)
        coEvery { userManager.lockUserAccount(userId, lockoutTime) } returns AppResult.Success(mockk())

        val result = service.recordFailedAttempt(testIdentifier, LockoutAttemptType.OTP, userId)

        assertTrue(result is AppResult.Error)
        val error = (result as AppResult.Error).error
        assertTrue(error is UserError.UserLocked)
        val userLocked = error as UserError.UserLocked
        assertEquals(userId, userLocked.userId)
        assertEquals(AccountLockoutType.TEMPORARY, userLocked.lockoutType)
        assertEquals(lockoutTime, userLocked.temporaryLockoutUntil)

        coVerify(exactly = 1) { userManager.lockUserAccount(userId, lockoutTime) }
    }

    @Test
    fun `recordFailedAttempt returns Error UserLocked without DB lock when userId is null`() = runTest {
        val lockoutTime = Clock.System.now() + 10.minutes

        coEvery {
            lockoutManager.recordFailedAttempt(testIdentifier, LockoutAttemptType.PASSWORD)
        } returns AppResult.Success(lockoutTime)

        val result = service.recordFailedAttempt(testIdentifier, LockoutAttemptType.PASSWORD, userId = null)

        assertTrue(result is AppResult.Error)
        val error = (result as AppResult.Error).error
        assertTrue(error is UserError.UserLocked)
        val userLocked = error as UserError.UserLocked
        assertEquals(null, userLocked.userId)
        assertEquals(AccountLockoutType.TEMPORARY, userLocked.lockoutType)
        assertEquals(lockoutTime, userLocked.temporaryLockoutUntil)

        coVerify(exactly = 0) { userManager.lockUserAccount(any(), any()) }
    }

    @Test
    fun `clearLockout clears attempt counters in LockoutManager for all identifiers`() = runTest {
        val identifiers = listOf(testIdentifier, secondIdentifier)
        coEvery { lockoutManager.clearLockout(testIdentifier) } returns AppResult.Success(Unit)
        coEvery { lockoutManager.clearLockout(secondIdentifier) } returns AppResult.Success(Unit)

        val result = service.clearLockout(identifiers)

        assertTrue(result is AppResult.Success)
        coVerify(exactly = 1) { lockoutManager.clearLockout(testIdentifier) }
        coVerify(exactly = 1) { lockoutManager.clearLockout(secondIdentifier) }
    }
}
