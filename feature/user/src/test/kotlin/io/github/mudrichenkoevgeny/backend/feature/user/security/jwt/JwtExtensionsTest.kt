package io.github.mudrichenkoevgeny.backend.feature.user.security.jwt

import io.github.mudrichenkoevgeny.backend.core.common.model.UserId
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.ktor.server.auth.jwt.JWTPrincipal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm

class JwtExtensionsTest {

    @Test
    fun `getUserIdFromSubject returns error for null blank or invalid subject`() {
        val cases = listOf(null, "", "   ", "not-a-uuid")

        cases.forEach { subject ->
            val result = getUserIdFromSubject(subject)
            assertTrue(result is AppResult.Error)
            assertTrue((result as AppResult.Error).error is UserError.InvalidAccessToken)
        }
    }

    @Test
    fun `getUserIdFromSubject returns user id for valid subject`() {
        val userId = UserId.generate()

        val result = getUserIdFromSubject(userId.asHexDashString())

        assertTrue(result is AppResult.Success)
        assertEquals(userId, (result as AppResult.Success).data)
    }

    @Test
    fun `getUserIdForWebSocket returns error when not optional and user id missing`() {
        val principal = principalWithSubject(subject = null)

        val result = principal.getUserIdForWebSocket(isOptional = false)

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is UserError.InvalidAccessToken)
    }

    @Test
    fun `getUserIdForWebSocket returns null when optional and user id missing`() {
        val principal = principalWithSubject(subject = null)

        val result = principal.getUserIdForWebSocket(isOptional = true)

        assertTrue(result is AppResult.Success)
        assertEquals(null, (result as AppResult.Success).data)
    }

    private fun principalWithSubject(subject: String?): JWTPrincipal {
        val payload = JWT.create()
            .withSubject(subject)
            .sign(Algorithm.HMAC256(SECRET))
            .let { token -> JWT.decode(token) }

        return JWTPrincipal(payload)
    }

    private companion object {
        const val SECRET = "01234567890123456789012345678901"
    }
}

