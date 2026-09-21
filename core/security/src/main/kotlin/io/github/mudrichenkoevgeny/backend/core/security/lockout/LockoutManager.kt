package io.github.mudrichenkoevgeny.backend.core.security.lockout

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import kotlin.time.Instant

/**
 * Redis-backed service for tracking authentication failed attempt counters and lockout states.
 *
 * Tracking keys (`identifier`) represent unique string keys in Redis. Depending on the authentication
 * or security workflow, an identifier can be a specific login credential (e.g., email address, phone number)
 * or a user ID UUID string for account-level, MFA, or TOTP security checks.
 */
interface LockoutManager {

    /**
     * Records a failed authentication attempt of [type] for the specified [identifier].
     *
     * Increments the attempt counter in Redis. If the count reaches or exceeds the allowed threshold
     * for [type], sets a lockout key in Redis and tracks consecutive temporary lockouts.
     *
     * @param identifier Unique key string for tracking attempts (e.g., login email, phone number, or user ID UUID string).
     * @param type The attempt type ([LockoutAttemptType.PASSWORD], [LockoutAttemptType.OTP], or [LockoutAttemptType.TOTP]).
     * @return [AppResult.Success] containing the lockout expiration [Instant] if locked, or `null` if not locked.
     */
    suspend fun recordFailedAttempt(
        identifier: String,
        type: LockoutAttemptType
    ): AppResult<Instant?>

    /**
     * Checks if the specified [identifier] is currently locked in Redis.
     *
     * @param identifier Unique key string for tracking attempts (e.g., login email, phone number, or user ID UUID string).
     * @return [AppResult.Success] with `true` if locked, `false` otherwise.
     */
    suspend fun isLocked(
        identifier: String
    ): AppResult<Boolean>

    /**
     * Checks if the specified [identifier] has reached the indefinite lockout threshold.
     *
     * @param identifier Unique key string for tracking attempts (e.g., login email, phone number, or user ID UUID string).
     * @return [AppResult.Success] with `true` if under indefinite lockout, `false` otherwise.
     */
    suspend fun isIndefiniteLockout(
        identifier: String
    ): AppResult<Boolean>

    /**
     * Retrieves the lockout expiration timestamp for the specified [identifier] if it is locked.
     *
     * @param identifier Unique key string for tracking attempts (e.g., login email, phone number, or user ID UUID string).
     * @return [AppResult.Success] with the [Instant] when lockout expires, or `null` if not locked.
     */
    suspend fun getLockoutUntil(
        identifier: String
    ): AppResult<Instant?>

    /**
     * Clears all failed attempt counters, consecutive lockouts, indefinite lockout state, and lockout state in Redis for the specified [identifier].
     *
     * @param identifier Unique key string for tracking attempts (e.g., login email, phone number, or user ID UUID string).
     * @return [AppResult.Success] with [Unit] or an error.
     */
    suspend fun clearLockout(
        identifier: String
    ): AppResult<Unit>
}
