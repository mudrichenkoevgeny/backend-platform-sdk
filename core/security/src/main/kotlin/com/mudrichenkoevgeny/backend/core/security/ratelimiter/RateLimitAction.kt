package com.mudrichenkoevgeny.backend.core.security.ratelimiter

enum class RateLimitAction(
    val id: String,
    val limit: Int,
    val windowSeconds: Int
) {
    SEND_OTP_EMAIL("send_otp", limit = 3, windowSeconds = 200),

    SEND_OTP_PHONE("send_otp", limit = 3, windowSeconds = 300),

    LOGIN_ATTEMPT("login", limit = 5, windowSeconds = 60),

    LOGOUT_ATTEMPT("logout", limit = 10, windowSeconds = 60),

    REGISTRATION_ATTEMPT("registration", limit = 5, windowSeconds = 60),

    PASSWORD_CHANGE("password_change", limit = 3, windowSeconds = 300),

    USER_IDENTIFIER_CHANGE("user_identifier_change", limit = 5, windowSeconds = 60),

    REFRESH_TOKEN("refresh", limit = 10, windowSeconds = 60),

    USER_DELETE("user_delete", limit = 3, windowSeconds = 60);

    fun createKey(identifier: String): String = "rl:$id:$identifier"
}