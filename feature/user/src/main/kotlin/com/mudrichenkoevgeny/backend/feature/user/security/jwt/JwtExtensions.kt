package com.mudrichenkoevgeny.backend.feature.user.security.jwt

import com.auth0.jwt.exceptions.JWTDecodeException
import com.mudrichenkoevgeny.backend.core.common.model.UserId
import com.mudrichenkoevgeny.backend.core.common.model.UserSessionId
import com.mudrichenkoevgeny.backend.core.common.model.toUserIdOrNull
import com.mudrichenkoevgeny.backend.core.common.model.toUserSessionIdOrNull
import com.mudrichenkoevgeny.backend.core.common.result.AppResult
import com.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import com.mudrichenkoevgeny.backend.feature.user.network.constants.UserNetworkConstants
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jws
import io.jsonwebtoken.JwtBuilder
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.PipelineCall
import io.ktor.server.auth.jwt.JWTCredential
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal

/** JwtBuilder **/
fun JwtBuilder.withUserIdSubject(userId: UserId): JwtBuilder {
    return this.subject(userId.value.toString())
}

fun JwtBuilder.withSessionIdSubject(sessionId: UserSessionId): JwtBuilder {
    return this.claim(UserNetworkConstants.SESSION_ID_CLAIM, sessionId.value.toString())
}

/** JWTCredential **/
fun JWTCredential.getUserId(): UserId {
    val jwtDecodeException = JWTDecodeException("Invalid subject")

    val subject = this.payload.subject
    if (subject.isNullOrBlank()) {
        throw jwtDecodeException
    }

    return subject.toUserIdOrNull() ?: throw jwtDecodeException
}

fun JWTCredential.getSessionId(): UserSessionId? {
    val sessionId = this.getClaim(UserNetworkConstants.SESSION_ID_CLAIM, String::class)
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
    val sessionId = this.getClaim(UserNetworkConstants.SESSION_ID_CLAIM, String::class)
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