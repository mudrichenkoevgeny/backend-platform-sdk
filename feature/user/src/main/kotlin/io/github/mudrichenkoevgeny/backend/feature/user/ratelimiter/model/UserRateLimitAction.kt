package io.github.mudrichenkoevgeny.backend.feature.user.ratelimiter.model

import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.model.RateLimitAction

/**
 * Rate limit policies for user-facing authentication and security operations.
 */
enum class UserRateLimitAction(
    override val id: String,
    override val limit: Int,
    override val windowSeconds: Int
) : RateLimitAction {
    SEND_OTP_EMAIL("send_otp_email", limit = 3, windowSeconds = 200),
    SEND_OTP_PHONE("send_otp_phone", limit = 3, windowSeconds = 300),
    LOGIN_ATTEMPT("login", limit = 5, windowSeconds = 60),
    REGISTRATION_ATTEMPT("registration", limit = 5, windowSeconds = 60),
    PASSWORD_CHANGE("password_change", limit = 3, windowSeconds = 300),
    USER_IDENTIFIER_ADD("user_identifier_add", limit = 5, windowSeconds = 60),
    USER_IDENTIFIER_DELETE("user_identifier_delete", limit = 5, windowSeconds = 60),
    REFRESH_TOKEN("refresh", limit = 10, windowSeconds = 60),
    USER_SCHEDULE_DELETION("user_schedule_deletion", limit = 3, windowSeconds = 60),
    USER_RESTORE("user_restore", limit = 5, windowSeconds = 60),
    SESSION_DELETE("session_delete", limit = 5, windowSeconds = 60),
    USER_SETUP_TOTP("user_setup_totp", limit = 5, windowSeconds = 300),
    USER_ENABLE_TOTP("user_enable_totp", limit = 5, windowSeconds = 300),
    USER_DISABLE_TOTP("user_disable_totp", limit = 5, windowSeconds = 300),
    USER_GET_RECOVERY_CODES("user_get_recovery_codes", limit = 10, windowSeconds = 60),
    USER_REGENERATE_RECOVERY_CODES("user_regenerate_recovery_codes", limit = 5, windowSeconds = 300),
    SESSION_REAUTHENTICATE("session_reauthenticate", limit = 5, windowSeconds = 60),
}