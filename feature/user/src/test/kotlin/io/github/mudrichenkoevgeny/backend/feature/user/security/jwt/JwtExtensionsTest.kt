package io.github.mudrichenkoevgeny.backend.feature.user.security.jwt

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTDecodeException
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.network.contract.UserTokenClaims
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jws
import io.jsonwebtoken.Jwts
import io.ktor.server.auth.jwt.JWTCredential
import io.ktor.server.auth.jwt.JWTPrincipal
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Date

class JwtExtensionsTest {

    private companion object {
        const val SECRET = "01234567890123456789012345678901"
        val ALGORITHM: Algorithm = Algorithm.HMAC256(SECRET)
    }

    @Test
    fun `JwtBuilder extensions should set correct claims`() {
        val userId = UserId.generate()
        val sessionId = UserSessionId.generate()
        val userRole = UserRole.ADMIN

        val token = Jwts.builder()
            .withUserIdSubject(userId)
            .withSessionIdSubject(sessionId)
            .withUserRoleSubject(userRole)
            .signWith(Jwts.SIG.HS256.key().build())
            .compact()

        val payload = JWT.decode(token)
        assertEquals(userId.asHexDashString(), payload.subject)
        assertEquals(sessionId.asHexDashString(), payload.getClaim(UserTokenClaims.SESSION_ID).asString())
        assertEquals(userRole.serialName, payload.getClaim(UserTokenClaims.USER_ROLE).asString())
    }

    @Test
    fun `JWTCredential getUserIdFromCredential should return userId or throw`() {
        val userId = UserId.generate()
        val credential = createCredential(subject = userId.asHexDashString())
        val invalidCredential = createCredential(subject = "invalid")
        val missingCredential = createCredential(subject = null)

        assertEquals(userId, credential.getUserIdFromCredential())
        assertThrows<JWTDecodeException> { invalidCredential.getUserIdFromCredential() }
        assertThrows<JWTDecodeException> { missingCredential.getUserIdFromCredential() }
    }

    @Test
    fun `JWTCredential getSessionIdFromCredential should return session id or null`() {
        val sessionId = UserSessionId.generate()
        val credential = createCredential(claims = mapOf(UserTokenClaims.SESSION_ID to sessionId.asHexDashString()))
        val missingCredential = createCredential()

        assertEquals(sessionId, credential.getSessionIdFromCredential())
        assertNull(missingCredential.getSessionIdFromCredential())
    }

    @Test
    fun `JWTPrincipal extensions should extract data correctly`() {
        val userId = UserId.generate()
        val sessionId = UserSessionId.generate()
        val userRole = UserRole.USER
        val expMillis = (System.currentTimeMillis() / 1000) * 1000 + 10000
        val exp = Date(expMillis)

        val principal = createPrincipal(
            subject = userId.asHexDashString(),
            expiresAt = exp,
            claims = mapOf(
                UserTokenClaims.SESSION_ID to sessionId.asHexDashString(),
                UserTokenClaims.USER_ROLE to userRole.serialName
            )
        )

        assertEquals(userId, principal.getUserId())
        assertEquals(sessionId, principal.getSessionId())
        assertEquals(userRole, principal.getUserRole())
        assertEquals(exp.time, principal.getExpiresAt())
    }

    @Test
    fun `getUserIdFromSubject should validate subject properly`() {
        val userId = UserId.generate()

        assertTrue(getUserIdFromSubject(userId.asHexDashString()) is AppResult.Success)

        val errorCases = listOf(null, "", "  ", "random-string")
        errorCases.forEach {
            val result = getUserIdFromSubject(it)
            assertTrue(result is AppResult.Error)
            assertTrue((result as AppResult.Error).error is UserError.InvalidAccessToken)
        }
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun `Jws Claims getUserIdFromPayload should extract user id`() {
        val userId = UserId.generate()
        val claims = mockk<Claims>()
        val jws = mockk<Jws<Claims>>()

        every { jws.payload } returns claims
        every { claims.subject } returns userId.asHexDashString()

        val result = jws.getUserIdFromPayload()
        assertEquals(userId, (result as AppResult.Success).data)
    }

    @Test
    fun `getUserIdForWebSocket should handle optional flag`() {
        val userId = UserId.generate()
        val validPrincipal = createPrincipal(subject = userId.asHexDashString())
        val emptyPrincipal = createPrincipal(subject = null)

        assertEquals(userId, (validPrincipal.getUserIdForWebSocket(false) as AppResult.Success).data)

        val requiredError = emptyPrincipal.getUserIdForWebSocket(isOptional = false)
        assertTrue(requiredError is AppResult.Error)

        val optionalSuccess = emptyPrincipal.getUserIdForWebSocket(isOptional = true)
        assertTrue(optionalSuccess is AppResult.Success)
        assertNull((optionalSuccess as AppResult.Success).data)
    }

    private fun createCredential(
        subject: String? = null,
        claims: Map<String, String> = emptyMap()
    ): JWTCredential {
        val builder = JWT.create().withSubject(subject)
        claims.forEach { (k, v) -> builder.withClaim(k, v) }
        return JWTCredential(builder.sign(ALGORITHM).let { JWT.decode(it) })
    }

    private fun createPrincipal(
        subject: String? = null,
        expiresAt: Date? = null,
        claims: Map<String, String> = emptyMap()
    ): JWTPrincipal {
        val builder = JWT.create().withSubject(subject)
        expiresAt?.let { builder.withExpiresAt(it) }
        claims.forEach { (k, v) -> builder.withClaim(k, v) }
        return JWTPrincipal(builder.sign(ALGORITHM).let { JWT.decode(it) })
    }
}