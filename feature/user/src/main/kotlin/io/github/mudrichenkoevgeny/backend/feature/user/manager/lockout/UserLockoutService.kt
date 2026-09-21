package io.github.mudrichenkoevgeny.backend.feature.user.manager.lockout

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.lockout.LockoutAttemptType
import io.github.mudrichenkoevgeny.backend.core.security.lockout.LockoutManager
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId

/**
 * Orchestrates account lockout checks and enforcement across user authentication flows.
 *
 * Coordinates tracking limits with [LockoutManager] and persisting lockout states on user
 * entities via [UserManager].
 */
interface UserLockoutService {

    /**
     * Checks if the given identifier is currently locked out.
     *
     * @param identifier Unique key string used for lockout tracking (e.g., login email, phone number, or user ID UUID string).
     * @param userId Optional user ID to persist lockout state in storage if locked out.
     * @return [AppResult.Success] if clear, or [AppResult.Error] with [UserError.UserLocked] if locked out.
     */
    suspend fun checkLockout(
        identifier: String,
        userId: UserId? = null
    ): AppResult<Unit>

    /**
     * Checks if any of the provided identifiers are currently locked out.
     *
     * @param identifiers List of unique key strings used for lockout tracking (e.g., login email, phone number, or user ID UUID string).
     * @param userId Optional user ID to persist lockout state in storage if locked out.
     * @return [AppResult.Success] if clear, or [AppResult.Error] with [UserError.UserLocked] if locked out.
     */
    suspend fun checkLockout(
        identifiers: List<String>,
        userId: UserId? = null
    ): AppResult<Unit>

    /**
     * Records a failed authentication attempt for a single identifier.
     *
     * @param identifier Unique key string used for tracking failed attempts (e.g., login email, phone number, or user ID UUID string).
     * @param type Type of failed attempt ([LockoutAttemptType.PASSWORD], [LockoutAttemptType.OTP], [LockoutAttemptType.TOTP]).
     * @param userId Optional user ID to persist lockout state in storage if a lockout is triggered.
     * @return [AppResult.Success] if no lockout was triggered, or [AppResult.Error] with [UserError.UserLocked] if lockout triggered.
     */
    suspend fun recordFailedAttempt(
        identifier: String,
        type: LockoutAttemptType,
        userId: UserId? = null
    ): AppResult<Unit>

    /**
     * Records a failed authentication attempt for multiple identifiers.
     *
     * @param identifiers List of unique key strings used for tracking failed attempts (e.g., login email, phone number, or user ID UUID string).
     * @param type Type of failed attempt ([LockoutAttemptType.PASSWORD], [LockoutAttemptType.OTP], [LockoutAttemptType.TOTP]).
     * @param userId Optional user ID to persist lockout state in storage if a lockout is triggered.
     * @return [AppResult.Success] if no lockout was triggered, or [AppResult.Error] with [UserError.UserLocked] if lockout triggered.
     */
    suspend fun recordFailedAttempt(
        identifiers: List<String>,
        type: LockoutAttemptType,
        userId: UserId? = null
    ): AppResult<Unit>

    /**
     * Clears all lockout attempt counters for the given identifiers.
     *
     * @param identifiers List of unique key strings to clear from lockout tracking (e.g., login email, phone number, or user ID UUID string).
     * @return [AppResult.Success] upon clearing.
     */
    suspend fun clearLockout(
        identifiers: List<String>
    ): AppResult<Unit>
}
