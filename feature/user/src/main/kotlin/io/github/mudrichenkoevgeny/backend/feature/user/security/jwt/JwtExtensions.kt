package io.github.mudrichenkoevgeny.backend.feature.user.security.jwt

import com.auth0.jwt.exceptions.JWTDecodeException
import io.github.mudrichenkoevgeny.backend.core.common.model.UserId
import io.github.mudrichenkoevgeny.backend.core.common.model.UserSessionId
import io.github.mudrichenkoevgeny.backend.core.common.model.toUserIdOrNull
import io.github.mudrichenkoevgeny.backend.core.common.model.toUserSessionIdOrNull
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.network.contract.UserTokenClaims
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

/** JJWT builder **/
fun JwtBuilder.withUserIdSubject(userId: UserId): JwtBuilder {
    return this.subject(userId.asHexDashString())
}

fun JwtBuilder.withSessionIdSubject(sessionId: UserSessionId): JwtBuilder {
    return this.claim(UserTokenClaims.SESSION_ID, sessionId.asHexDashString())
}

/** JWTCredential **/
/**
 * Extracts a user id from the token subject.
 *
 * @throws JWTDecodeException when the subject is missing or cannot be parsed as a user id
 */
fun JWTCredential.getUserId(): UserId {
    return payload.subject?.toUserIdOrNull()
        ?: throw JWTDecodeException("Invalid or missing subject")
}

/**
 * Extracts an optional user session id from a token claim.
 *
 * @return parsed session id, or `null` when claim is missing or invalid
 */
fun JWTCredential.getSessionId(): UserSessionId? {
    val sessionId = this.getClaim(UserTokenClaims.SESSION_ID, String::class)
        ?: return null
    return sessionId.toUserSessionIdOrNull()
}

/** JWTPrincipal **/
/**
 * Returns the current call JWT principal, when Ktor authentication has been applied.
 */
fun ApplicationCall.getJWTPrincipal(): JWTPrincipal? {
    return this.principal<JWTPrincipal>()
}

/**
 * Reads a user id from the principal subject.
 *
 * @return parsed user id or `null` if missing/invalid
 */
fun JWTPrincipal.getUserId(): UserId? {
    val subject = this.payload.subject
    return subject?.toUserIdOrNull()
}

/**
 * Reads a user session id from the principal claim.
 *
 * @return parsed session id or `null` if missing/invalid
 */
fun JWTPrincipal.getSessionId(): UserSessionId? {
    val sessionId = this.getClaim(UserTokenClaims.SESSION_ID, String::class)
        ?: return null
    return sessionId.toUserSessionIdOrNull()
}

/** UserId, UserSessionId **/
/**
 * Extracts a required user id from the JWT subject of the current call.
 */
fun ApplicationCall.getUserIdFromPayload(): AppResult<UserId> {
    val principal = this.getJWTPrincipal()
        ?: return AppResult.Error(UserError.InvalidAccessToken())

    return getUserIdFromSubject(principal.payload.subject)
}

/**
 * Extracts a required user id from the JWT subject of parsed signed claims.
 */
fun Jws<Claims>.getUserIdFromPayload(): AppResult<UserId> {
    return getUserIdFromSubject(this.payload.subject)
}

/**
 * Parses [subject] into a [UserId] and maps invalid inputs to [UserError.InvalidAccessToken].
 */
fun getUserIdFromSubject(subject: String?): AppResult<UserId> {
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
fun JWTPrincipal.getUserIdForWebSocket(isOptional: Boolean = false): AppResult<UserId?> {
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

/** Alias for readability in WebSocket code. */
fun JWTPrincipal.getUserSessionId(): UserSessionId? {
    return this.getSessionId()
}

/**
 * Returns the token expiration instant as epoch millis.
 */
fun JWTPrincipal.getExpiresAt(): Long? {
    return this.payload.expiresAt?.time
}