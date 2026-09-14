package io.github.mudrichenkoevgeny.backend.core.security.lockout

/**
 * Supported authentication attempt types for tracking brute-force lockouts.
 */
enum class LockoutAttemptType {
    PASSWORD,
    OTP,
    TOTP
}
