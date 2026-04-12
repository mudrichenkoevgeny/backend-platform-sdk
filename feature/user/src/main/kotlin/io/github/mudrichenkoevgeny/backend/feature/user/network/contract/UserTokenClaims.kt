package io.github.mudrichenkoevgeny.backend.feature.user.network.contract

/**
 * JWT claim names used by the user feature.
 *
 * These keys are written into access tokens and later read by Ktor authentication
 * and WebSocket infrastructure.
 */
object UserTokenClaims {
    /**
     * Identifier of the authenticated user session.
     */
    const val SESSION_ID = "sessionId"

    const val USER_ROLE = "userRole"
}