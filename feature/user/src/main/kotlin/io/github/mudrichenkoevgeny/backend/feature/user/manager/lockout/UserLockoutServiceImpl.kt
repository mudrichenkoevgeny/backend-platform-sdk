package io.github.mudrichenkoevgeny.backend.feature.user.manager.lockout

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.lockout.LockoutAttemptType
import io.github.mudrichenkoevgeny.backend.core.security.lockout.LockoutManager
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.accountlockout.AccountLockoutType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserLockoutServiceImpl @Inject constructor(
    private val lockoutManager: LockoutManager,
    private val userManager: UserManager
) : UserLockoutService {

    override suspend fun checkLockout(
        identifier: String,
        userId: UserId?
    ): AppResult<Unit> = checkLockout(listOf(identifier), userId)

    override suspend fun checkLockout(
        identifiers: List<String>,
        userId: UserId?
    ): AppResult<Unit> {
        val isIndefinite = identifiers.any { id ->
            (lockoutManager.isIndefiniteLockout(id) as? AppResult.Success)?.data == true
        }

        if (isIndefinite) {
            if (userId != null) {
                userManager.lockUserAccountIndefinitely(userId)
            }
            return AppResult.Error(
                UserError.UserLocked(
                    userId = userId,
                    lockoutType = AccountLockoutType.INDEFINITE
                )
            )
        }

        val maxLockoutUntil = identifiers.mapNotNull { id ->
            (lockoutManager.getLockoutUntil(id) as? AppResult.Success)?.data
        }.maxOrNull()

        if (maxLockoutUntil != null) {
            return AppResult.Error(
                UserError.UserLocked(
                    userId = userId,
                    lockoutType = AccountLockoutType.TEMPORARY,
                    temporaryLockoutUntil = maxLockoutUntil
                )
            )
        }

        return AppResult.Success(Unit)
    }

    override suspend fun recordFailedAttempt(
        identifier: String,
        type: LockoutAttemptType,
        userId: UserId?
    ): AppResult<Unit> = recordFailedAttempt(listOf(identifier), type, userId)

    override suspend fun recordFailedAttempt(
        identifiers: List<String>,
        type: LockoutAttemptType,
        userId: UserId?
    ): AppResult<Unit> {
        val lockouts = identifiers.mapNotNull { id ->
            (lockoutManager.recordFailedAttempt(id, type) as? AppResult.Success)?.data
        }
        val maxLockoutUntil = lockouts.maxOrNull()

        if (maxLockoutUntil != null) {
            if (userId != null) {
                userManager.lockUserAccount(userId, maxLockoutUntil)
            }
            return AppResult.Error(
                UserError.UserLocked(
                    userId = userId,
                    lockoutType = AccountLockoutType.TEMPORARY,
                    temporaryLockoutUntil = maxLockoutUntil
                )
            )
        }

        return AppResult.Success(Unit)
    }

    override suspend fun clearLockout(identifiers: List<String>): AppResult<Unit> {
        identifiers.forEach { id ->
            lockoutManager.clearLockout(id)
        }
        return AppResult.Success(Unit)
    }
}
