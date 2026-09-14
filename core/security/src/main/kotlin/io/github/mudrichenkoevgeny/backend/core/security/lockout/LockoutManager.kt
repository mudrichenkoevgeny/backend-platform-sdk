package io.github.mudrichenkoevgeny.backend.core.security.lockout

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import kotlin.time.Instant

/**
 * Redis-backed service for tracking authentication failed attempt counters and lockout state.
 */
interface LockoutManager {

    /**
     * Records a failed authentication attempt of [type] for the specified [identifier].
     *
     * Increments the attempt counter in Redis. If the count reaches or exceeds the allowed threshold
     * for [type], sets a lockout key in Redis and tracks consecutive temporary lockouts.
     *
     * @param identifier Unique identifier (e.g. email, phone, or user ID).
     * @param type The attempt type (PASSWORD, OTP, or TOTP).
     * @return [AppResult.Success] containing the lockout expiration [Instant] if locked, or `null` if not locked.
     */
    suspend fun recordFailedAttempt(
        identifier: String,
        type: LockoutAttemptType
    ): AppResult<Instant?>

    /**
     * Checks if the specified [identifier] is currently locked in Redis.
     *
     * @param identifier Unique identifier.
     * @return [AppResult.Success] with `true` if locked, `false` otherwise.
     */
    suspend fun isLocked(
        identifier: String
    ): AppResult<Boolean>

    /**
     * Checks if the specified [identifier] has reached the indefinite lockout threshold.
     *
     * @param identifier Unique identifier.
     * @return [AppResult.Success] with `true` if under indefinite lockout, `false` otherwise.
     */
    suspend fun isIndefiniteLockout(
        identifier: String
    ): AppResult<Boolean>

    /**
     * Retrieves the lockout expiration timestamp for the specified [identifier] if it is locked.
     *
     * @param identifier Unique identifier.
     * @return [AppResult.Success] with the [Instant] when lockout expires, or `null` if not locked.
     */
    suspend fun getLockoutUntil(
        identifier: String
    ): AppResult<Instant?>

    /**
     * Clears all failed attempt counters, consecutive lockouts, indefinite lockout state, and lockout state in Redis for the specified [identifier].
     *
     * @param identifier Unique identifier.
     * @return [AppResult.Success] with [Unit] or an error.
     */
    suspend fun clearLockout(
        identifier: String
    ): AppResult<Unit>
}