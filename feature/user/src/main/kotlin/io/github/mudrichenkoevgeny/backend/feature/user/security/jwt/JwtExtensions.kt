package io.github.mudrichenkoevgeny.backend.feature.user.security.jwt

import com.auth0.jwt.exceptions.JWTDecodeException
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.network.contract.UserTokenClaims
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.toUserIdentifierIdOrNull
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.toUserSessionIdOrNull
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.toUserIdOrNull
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jws
import io.jsonwebtoken.JwtBuilder
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTCredential
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal

/**
 * JWT helpers for the user feature.
 *
 * This file groups:
 * - JJWT builder extensions for issuing tokens,
 * - Ktor JWT credential/principal helpers for extracting user/session identifiers,
 * - shared parsing helpers used by both HTTP and WebSocket flows.
 */

public fun JwtBuilder.withUserIdSubject(userId: UserId): JwtBuilder = this.subject(userId.asHexDashString())

/**
 * Adds a session ID claim to the JWT payload.
 *
 * @param sessionId The session ID to include.
 * @return The updated [JwtBuilder].
 */
public fun JwtBuilder.withSessionIdSubject(sessionId: UserSessionId): JwtBuilder = this.claim(
    UserTokenClaims.SESSION_ID,
    sessionId.asHexDashString()
)

/**
 * Adds an identifier ID claim to the JWT payload.
 *
 * @param identifierId The identifier ID to include.
 * @return The updated [JwtBuilder].
 */
public fun JwtBuilder.withIdentifierIdClaim(identifierId: UserIdentifierId): JwtBuilder = this.claim(
    UserTokenClaims.IDENTIFIER_ID,
    identifierId.asHexDashString()
)

/**
 * Adds a user role claim to the JWT payload.
 *
 * @param userRole The user role to include.
 * @return The updated [JwtBuilder].
 */
public fun JwtBuilder.withUserRoleSubject(userRole: UserRole): JwtBuilder = this.claim(
    UserTokenClaims.USER_ROLE,
    userRole.value
)

/**
 * Extracts a user id from the token subject.
 *
 * @throws JWTDecodeException when the subject is missing or cannot be parsed as a user id
 */
public fun JWTCredential.getUserIdFromCredential(): UserId =
    payload.subject?.toUserIdOrNull() ?: throw JWTDecodeException("Invalid or missing subject")

/**
 * Extracts an optional user session id from a token claim.
 *
 * @return parsed session id, or `null` when claim is missing or invalid
 */
public fun JWTCredential.getSessionIdFromCredential(): UserSessionId? {
    val sessionId = this.getClaim(
        UserTokenClaims.SESSION_ID,
        String::class
    ) ?: return null
    return sessionId.toUserSessionIdOrNull()
}

/**
 * Extracts an optional user identifier ID from a token claim.
 *
 * @return parsed identifier ID, or `null` when claim is missing or invalid
 */
public fun JWTCredential.getIdentifierIdFromCredential(): UserIdentifierId? {
    val identifierId = this.getClaim(
        UserTokenClaims.IDENTIFIER_ID,
        String::class
    ) ?: return null
    return identifierId.toUserIdentifierIdOrNull()
}

/**
 * Returns the current call JWT principal, when Ktor authentication has been applied.
 */
public fun ApplicationCall.getJWTPrincipal(): JWTPrincipal? = this.principal<JWTPrincipal>()

/**
 * Reads a user id from the principal subject.
 *
 * @return parsed user id or `null` if missing/invalid
 */
public fun JWTPrincipal.getUserId(): UserId? {
    val subject = this.payload.subject
    return subject?.toUserIdOrNull()
}

/**
 * Reads a user role from the principal claim.
 *
 * @return parsed user role or `null` if missing/invalid
 */
public fun JWTPrincipal.getUserRole(): UserRole? {
    val userRole = this.getClaim(
        UserTokenClaims.USER_ROLE,
        String::class
    )
    return userRole?.let { userRole -> UserRole.fromValueOrNull(userRole) }
}

/**
 * Reads a user session id from the principal claim.
 *
 * @return parsed session id or `null` if missing/invalid
 */
public fun JWTPrincipal.getSessionId(): UserSessionId? {
    val sessionId = this.getClaim(
        UserTokenClaims.SESSION_ID,
        String::class
    ) ?: return null
    return sessionId.toUserSessionIdOrNull()
}

/**
 * Reads a user identifier id from the principal claim.
 *
 * @return parsed identifier id or `null` if missing/invalid
 */
public fun JWTPrincipal.getIdentifierId(): UserIdentifierId? {
    val identifierId = this.getClaim(
        UserTokenClaims.IDENTIFIER_ID,
        String::class
    ) ?: return null
    return identifierId.toUserIdentifierIdOrNull()
}

/**
 * Extracts a required user id from the JWT subject of the current call.
 */
public fun ApplicationCall.getUserIdFromPayload(): AppResult<UserId> {
    val principal = this.getJWTPrincipal() ?: return AppResult.Error(UserError.InvalidAccessToken())

    return getUserIdFromSubject(principal.payload.subject)
}

/**
 * Extracts a required user id from the JWT subject of parsed signed claims.
 */
public fun Jws<Claims>.getUserIdFromPayload(): AppResult<UserId> = getUserIdFromSubject(this.payload.subject)

/**
 * Parses [subject] into a [UserId] and maps invalid inputs to [UserError.InvalidAccessToken].
 */
public fun getUserIdFromSubject(subject: String?): AppResult<UserId> {
    val invalidAccessTokenErrorResult = AppResult.Error(UserError.InvalidAccessToken())

    if (subject.isNullOrBlank()) {
        return invalidAccessTokenErrorResult
    }

    val userId = subject.toUserIdOrNull() ?: return invalidAccessTokenErrorResult

    return AppResult.Success(userId)
}

/**
 * Resolves a user id for WebSocket registration.
 *
 * When [isOptional] is true, missing/invalid user id is allowed and results in `null`.
 * Otherwise, missing/invalid id results in [UserError.InvalidAccessToken].
 */
public fun JWTPrincipal.getUserIdForWebSocket(isOptional: Boolean = false): AppResult<UserId?> {
    val userId = this.getUserId()

    return if (userId != null) {
        AppResult.Success(userId)
    } else {
        if (isOptional) {
            AppResult.Success(null)
        } else {
            AppResult.Error(UserError.InvalidAccessToken())
        }
    }
}

/**
 * Returns the token expiration instant as epoch millis.
 */
public fun JWTPrincipal.getExpiresAt(): Long? = this.payload.expiresAt?.time
