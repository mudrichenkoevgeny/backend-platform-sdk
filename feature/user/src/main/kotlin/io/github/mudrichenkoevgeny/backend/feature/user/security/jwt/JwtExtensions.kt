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

/** JwtBuilder **/
fun JwtBuilder.withUserIdSubject(userId: UserId): JwtBuilder {
    return this.subject(userId.asHexDashString())
}

fun JwtBuilder.withSessionIdSubject(sessionId: UserSessionId): JwtBuilder {
    return this.claim(UserTokenClaims.SESSION_ID, sessionId.asHexDashString())
}

/** JWTCredential **/
fun JWTCredential.getUserId(): UserId {
    return payload.subject?.toUserIdOrNull()
        ?: throw JWTDecodeException("Invalid or missing subject")
}

fun JWTCredential.getSessionId(): UserSessionId? {
    val sessionId = this.getClaim(UserTokenClaims.SESSION_ID, String::class)
        ?: return null
    return sessionId.toUserSessionIdOrNull()
}

/** JWTPrincipal **/
fun ApplicationCall.getJWTPrincipal(): JWTPrincipal? {
    return this.principal<JWTPrincipal>()
}

fun JWTPrincipal.getUserId(): UserId? {
    val subject = this.payload.subject
    return subject.toUserIdOrNull()
}

fun JWTPrincipal.getSessionId(): UserSessionId? {
    val sessionId = this.getClaim(UserTokenClaims.SESSION_ID, String::class)
        ?: return null
    return sessionId.toUserSessionIdOrNull()
}

/** UserId, UserSessionId **/
fun ApplicationCall.getUserIdFromPayload(): AppResult<UserId> {
    val principal = this.getJWTPrincipal()
        ?: return AppResult.Error(UserError.InvalidAccessToken())

    return getUserIdFromSubject(principal.payload.subject)
}

fun Jws<Claims>.getUserIdFromPayload(): AppResult<UserId> {
    return getUserIdFromSubject(this.payload.subject)
}

fun getUserIdFromSubject(subject: String?): AppResult<UserId> {
    val invalidAccessTokenErrorResult = AppResult.Error(UserError.InvalidAccessToken())

    if (subject.isNullOrBlank()) {
        return invalidAccessTokenErrorResult
    }

    val userId = subject.toUserIdOrNull() ?: return invalidAccessTokenErrorResult

    return AppResult.Success(userId)
}

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

fun JWTPrincipal.getUserSessionId(): UserSessionId? {
    return this.getSessionId()
}

fun JWTPrincipal.getExpiresAt(): Long? {
    return this.payload.expiresAt?.time
}