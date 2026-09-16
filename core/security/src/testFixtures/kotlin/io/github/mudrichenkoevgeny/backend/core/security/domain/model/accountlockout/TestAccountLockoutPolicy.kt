package io.github.mudrichenkoevgeny.backend.core.security.domain.model.accountlockout

import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.accountlockout.AccountLockoutPolicy

/**
 * Test factory for creating [AccountLockoutPolicy] instances in tests.
 */
fun createTestAccountLockoutPolicy(
    maxFailedPasswordAttempts: Int = 5,
    maxFailedOtpAttempts: Int = 3,
    maxFailedTotpAttempts: Int = 3,
    failedAttemptsWindowSeconds: Int = 300,
    lockoutDurationSeconds: Int = 900,
    indefiniteLockoutThreshold: Int = 3,
    isSelfServiceUnlockEnabled: Boolean = true
) = AccountLockoutPolicy(
    maxFailedPasswordAttempts = maxFailedPasswordAttempts,
    maxFailedOtpAttempts = maxFailedOtpAttempts,
    maxFailedTotpAttempts = maxFailedTotpAttempts,
    failedAttemptsWindowSeconds = failedAttemptsWindowSeconds,
    lockoutDurationSeconds = lockoutDurationSeconds,
    indefiniteLockoutThreshold = indefiniteLockoutThreshold,
    isSelfServiceUnlockEnabled = isSelfServiceUnlockEnabled
)
