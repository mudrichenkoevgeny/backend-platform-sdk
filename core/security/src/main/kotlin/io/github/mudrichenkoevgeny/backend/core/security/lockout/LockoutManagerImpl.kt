package io.github.mudrichenkoevgeny.backend.core.security.lockout

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.database.manager.redis.RedisManager
import io.github.mudrichenkoevgeny.backend.core.security.settings.provider.SecuritySettingsProvider
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Redis-backed implementation of [LockoutManager].
 *
 * Uses Redis counters and key expiration to manage failed attempt windows, temporary lockouts,
 * consecutive temporary lockouts, and indefinite lockout thresholds.
 */
@Singleton
class LockoutManagerImpl @Inject constructor(
    private val redisManager: RedisManager,
    private val securitySettingsProvider: SecuritySettingsProvider
) : LockoutManager {

    override suspend fun recordFailedAttempt(
        identifier: String,
        type: LockoutAttemptType
    ): AppResult<Instant?> {
        val policy = securitySettingsProvider.getAccountLockoutPolicy()
        val maxAttempts = when (type) {
            LockoutAttemptType.PASSWORD -> policy.maxFailedPasswordAttempts
            LockoutAttemptType.OTP -> policy.maxFailedOtpAttempts
            LockoutAttemptType.TOTP -> policy.maxFailedTotpAttempts
        }

        val counterKey = buildCounterKey(identifier, type)
        val countResult = redisManager.incrementWithExpiration(
            key = counterKey,
            expirationSeconds = policy.failedAttemptsWindowSeconds.toLong()
        )

        val currentCount = when (countResult) {
            is AppResult.Success -> countResult.data
            is AppResult.Error -> return countResult
        }

        if (currentCount >= maxAttempts) {
            val lockoutDurationSeconds = policy.lockoutDurationSeconds.toLong()
            val lockoutUntil = Clock.System.now() + lockoutDurationSeconds.seconds
            val lockoutKey = buildLockoutKey(identifier)

            val setLockoutResult = redisManager.setWithExpiration(
                key = lockoutKey,
                value = lockoutUntil.toEpochMilliseconds().toString(),
                expirationSeconds = lockoutDurationSeconds
            )
            if (setLockoutResult is AppResult.Error) {
                return setLockoutResult
            }

            val consecutiveKey = buildConsecutiveKey(identifier)
            val consecutiveResult = redisManager.incrementWithExpiration(
                key = consecutiveKey,
                expirationSeconds = 604800L
            )
            val consecutiveCount = when (consecutiveResult) {
                is AppResult.Success -> consecutiveResult.data
                is AppResult.Error -> 0L
            }

            if (policy.permanentLockoutThreshold > 0 && consecutiveCount >= policy.permanentLockoutThreshold) {
                val indefiniteLockoutKey = buildIndefiniteLockoutKey(identifier)
                redisManager.setWithExpiration(
                    key = indefiniteLockoutKey,
                    value = "1",
                    expirationSeconds = 604800L
                )
            }

            return AppResult.Success(lockoutUntil)
        }

        return AppResult.Success(null)
    }

    override suspend fun isLocked(identifier: String): AppResult<Boolean> {
        val lockoutKey = buildLockoutKey(identifier)
        return redisManager.exists(lockoutKey)
    }

    override suspend fun isIndefiniteLockout(identifier: String): AppResult<Boolean> {
        val indefiniteLockoutKey = buildIndefiniteLockoutKey(identifier)
        return redisManager.exists(indefiniteLockoutKey)
    }

    override suspend fun getLockoutUntil(identifier: String): AppResult<Instant?> {
        val lockoutKey = buildLockoutKey(identifier)
        val ttlResult = redisManager.getTtl(lockoutKey)

        val ttl = when (ttlResult) {
            is AppResult.Success -> ttlResult.data
            is AppResult.Error -> return ttlResult
        }

        if (ttl <= 0) {
            return AppResult.Success(null)
        }

        val lockoutUntil = Clock.System.now() + ttl.seconds
        return AppResult.Success(lockoutUntil)
    }

    override suspend fun clearLockout(identifier: String): AppResult<Unit> {
        val lockoutKey = buildLockoutKey(identifier)
        val deleteLockoutResult = redisManager.delete(lockoutKey)
        if (deleteLockoutResult is AppResult.Error) {
            return deleteLockoutResult
        }

        val indefiniteLockoutKey = buildIndefiniteLockoutKey(identifier)
        redisManager.delete(indefiniteLockoutKey)

        val consecutiveKey = buildConsecutiveKey(identifier)
        redisManager.delete(consecutiveKey)

        for (type in LockoutAttemptType.entries) {
            val counterKey = buildCounterKey(identifier, type)
            val deleteCounterResult = redisManager.delete(counterKey)
            if (deleteCounterResult is AppResult.Error) {
                return deleteCounterResult
            }
        }

        return AppResult.Success(Unit)
    }

    private fun buildCounterKey(identifier: String, type: LockoutAttemptType): String {
        return "lockout:failed:${type.name.lowercase()}:$identifier"
    }

    private fun buildLockoutKey(identifier: String): String {
        return "lockout:blocked:$identifier"
    }

    private fun buildIndefiniteLockoutKey(identifier: String): String {
        return "lockout:indefinite:$identifier"
    }

    private fun buildConsecutiveKey(identifier: String): String {
        return "lockout:consecutive:$identifier"
    }
}