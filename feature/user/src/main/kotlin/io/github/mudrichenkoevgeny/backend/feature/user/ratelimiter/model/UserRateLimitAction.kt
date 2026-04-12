package io.github.mudrichenkoevgeny.backend.feature.user.ratelimiter.model

import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.model.RateLimitAction

/**
 * User-feature rate limit policies (login, OTP, registration, identifiers, etc.).
 *
 * Each entry defines:
 * - [id]: logical action identifier (used in storage keys and error details)
 * - [limit]: maximum allowed count during the time window
 * - [windowSeconds]: sliding window size in seconds (implemented as a key TTL)
 */
enum class UserRateLimitAction(
    override val id: String,
    override val limit: Int,
    override val windowSeconds: Int
) : RateLimitAction {
    SEND_OTP_EMAIL("send_otp", limit = 3, windowSeconds = 200),

    SEND_OTP_PHONE("send_otp", limit = 3, windowSeconds = 300),

    LOGIN_ATTEMPT("login", limit = 5, windowSeconds = 60),

    LOGOUT_ATTEMPT("logout", limit = 10, windowSeconds = 60),

    REGISTRATION_ATTEMPT("registration", limit = 5, windowSeconds = 60),

    PASSWORD_CHANGE("password_change", limit = 3, windowSeconds = 300),

    USER_IDENTIFIER_CHANGE("user_identifier_change", limit = 5, windowSeconds = 60),

    REFRESH_TOKEN("refresh", limit = 10, windowSeconds = 60),

    USER_DELETE("user_delete", limit = 3, windowSeconds = 60)
}
