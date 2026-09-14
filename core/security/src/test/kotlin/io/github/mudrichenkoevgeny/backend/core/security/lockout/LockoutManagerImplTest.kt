package io.github.mudrichenkoevgeny.backend.core.security.lockout

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.database.manager.redis.RedisManager
import io.github.mudrichenkoevgeny.backend.core.security.domain.model.accountlockout.createTestAccountLockoutPolicy
import io.github.mudrichenkoevgeny.backend.core.security.settings.provider.SecuritySettingsProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LockoutManagerImplTest {

    private val redisManager = mockk<RedisManager>()
    private val securitySettingsProvider = mockk<SecuritySettingsProvider>()
    private val lockoutManager = LockoutManagerImpl(redisManager, securitySettingsProvider)

    private val testPolicy = createTestAccountLockoutPolicy(permanentLockoutThreshold = 0)

    @BeforeEach
    fun setUp() {
        every { securitySettingsProvider.getAccountLockoutPolicy() } returns testPolicy
    }

    @Test
    fun `recordFailedAttempt returns null when count is below limit`() = runTest {
        val identifier = "test@example.com"
        val counterKey = "lockout:failed:password:$identifier"

        coEvery {
            redisManager.incrementWithExpiration(counterKey, testPolicy.failedAttemptsWindowSeconds.toLong())
        } returns AppResult.Success(1L)

        val result = lockoutManager.recordFailedAttempt(identifier, LockoutAttemptType.PASSWORD)

        assertTrue(result is AppResult.Success)
        assertNull((result as AppResult.Success).data)
        coVerify(exactly = 0) { redisManager.setWithExpiration(any(), any(), any()) }
    }

    @Test
    fun `recordFailedAttempt sets lockout key and returns expiration when limit is reached for OTP`() = runTest {
        val identifier = "+1234567890"
        val counterKey = "lockout:failed:otp:$identifier"
        val lockoutKey = "lockout:blocked:$identifier"
        val consecutiveKey = "lockout:consecutive:$identifier"

        coEvery {
            redisManager.incrementWithExpiration(counterKey, testPolicy.failedAttemptsWindowSeconds.toLong())
        } returns AppResult.Success(3L)

        coEvery {
            redisManager.setWithExpiration(lockoutKey, any(), testPolicy.lockoutDurationSeconds.toLong())
        } returns AppResult.Success(Unit)

        coEvery {
            redisManager.incrementWithExpiration(consecutiveKey, any())
        } returns AppResult.Success(1L)

        val result = lockoutManager.recordFailedAttempt(identifier, LockoutAttemptType.OTP)

        assertTrue(result is AppResult.Success)
        assertNotNull((result as AppResult.Success).data)
        coVerify(exactly = 1) {
            redisManager.setWithExpiration(lockoutKey, any(), testPolicy.lockoutDurationSeconds.toLong())
        }
    }

    @Test
    fun `isLocked returns true when lockout key exists`() = runTest {
        val identifier = "test@example.com"
        val lockoutKey = "lockout:blocked:$identifier"

        coEvery { redisManager.exists(lockoutKey) } returns AppResult.Success(true)

        val result = lockoutManager.isLocked(identifier)

        assertEquals(AppResult.Success(true), result)
    }

    @Test
    fun `isIndefiniteLockout returns true when indefinite lockout key exists`() = runTest {
        val identifier = "test@example.com"
        val indefiniteLockoutKey = "lockout:indefinite:$identifier"

        coEvery { redisManager.exists(indefiniteLockoutKey) } returns AppResult.Success(true)

        val result = lockoutManager.isIndefiniteLockout(identifier)

        assertEquals(AppResult.Success(true), result)
    }

    @Test
    fun `getLockoutUntil returns instant when ttl is positive`() = runTest {
        val identifier = "test@example.com"
        val lockoutKey = "lockout:blocked:$identifier"

        coEvery { redisManager.getTtl(lockoutKey) } returns AppResult.Success(600L)

        val result = lockoutManager.getLockoutUntil(identifier)

        assertTrue(result is AppResult.Success)
        assertNotNull((result as AppResult.Success).data)
    }

    @Test
    fun `getLockoutUntil returns null when ttl is negative`() = runTest {
        val identifier = "test@example.com"
        val lockoutKey = "lockout:blocked:$identifier"

        coEvery { redisManager.getTtl(lockoutKey) } returns AppResult.Success(-2L)

        val result = lockoutManager.getLockoutUntil(identifier)

        assertEquals(AppResult.Success(null), result)
    }

    @Test
    fun `clearLockout deletes lockout key and all attempt counters`() = runTest {
        val identifier = "test@example.com"

        coEvery { redisManager.delete(any()) } returns AppResult.Success(Unit)

        val result = lockoutManager.clearLockout(identifier)

        assertEquals(AppResult.Success(Unit), result)
        coVerify { redisManager.delete("lockout:blocked:$identifier") }
        coVerify { redisManager.delete("lockout:indefinite:$identifier") }
        coVerify { redisManager.delete("lockout:consecutive:$identifier") }
        coVerify { redisManager.delete("lockout:failed:password:$identifier") }
        coVerify { redisManager.delete("lockout:failed:otp:$identifier") }
        coVerify { redisManager.delete("lockout:failed:totp:$identifier") }
    }

    @Test
    fun `recordFailedAttempt returns error when redis increment fails`() = runTest {
        val identifier = "test@example.com"
        val counterKey = "lockout:failed:totp:$identifier"
        val error = CommonError.Internal(Throwable("Redis error"))

        coEvery {
            redisManager.incrementWithExpiration(counterKey, any())
        } returns AppResult.Error(error)

        val result = lockoutManager.recordFailedAttempt(identifier, LockoutAttemptType.TOTP)

        assertEquals(AppResult.Error(error), result)
    }
}
