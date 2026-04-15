package io.github.mudrichenkoevgeny.backend.feature.user.ratelimiter.model

import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.model.RateLimitAction

/**
 * Rate limit policies for management operations in the user feature.
 */
enum class UserManagementRateLimitAction(
    override val id: String,
    override val limit: Int,
    override val windowSeconds: Int
) : RateLimitAction {
    MANAGEMENT_IDENTIFIER_DELETE("management_identifier_delete", limit = 10, windowSeconds = 60),
    MANAGEMENT_SESSION_DELETE("management_session_delete", limit = 10, windowSeconds = 60),
    MANAGEMENT_SESSION_DELETE_ALL("management_session_delete_all", limit = 5, windowSeconds = 60)
}
