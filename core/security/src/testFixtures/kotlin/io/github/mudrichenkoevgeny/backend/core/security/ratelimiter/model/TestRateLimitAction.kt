package io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.model

enum class TestRateLimitAction(
    override val id: String,
    override val limit: Int,
    override val windowSeconds: Int
) : RateLimitAction {
    LOGIN_ATTEMPT("login", limit = 5, windowSeconds = 60),
    PASSWORD_CHANGE("password_change", limit = 3, windowSeconds = 300)
}
