package io.github.mudrichenkoevgeny.backend.feature.user.network.contract

/**
 * JWT claim names used by the user feature.
 *
 * These keys are written into access tokens and later read by Ktor authentication
 * and WebSocket infrastructure.
 */
public object UserTokenClaims {
    /**
     * Identifier of the authenticated user session.
     */
    public const val SESSION_ID: String = "sessionId"

    public const val IDENTIFIER_ID: String = "identifierId"

    public const val USER_ROLE: String = "userRole"
}
