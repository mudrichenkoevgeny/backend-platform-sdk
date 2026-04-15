package io.github.mudrichenkoevgeny.backend.feature.user.security.tokenprovider

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.config.model.UserConfig
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.security.refreshtokenprovider.RefreshTokenProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.settings.AvailableAuthProviders
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.settings.ManagementAuthSettings
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.token.AccessToken
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class JwtTokenProviderTest {

    private val refreshTokenProvider: RefreshTokenProvider = mockk(relaxed = true)
    private val provider = JwtTokenProvider(
        userConfig = userConfig(jwtSecret = SECRET),
        refreshTokenProvider = refreshTokenProvider
    )

    @Test
    fun `verifyAccessToken returns user id for valid token`() {
        val userId = UserId.generate()
        val sessionId = UserSessionId.generate()
        val now = Instant.now()

        val tokenResult = provider.generateAccessToken(
            userId = userId,
            userRole = UserRole.USER,
            sessionId = sessionId,
            issuedAt = now,
            expiration = now.plusSeconds(60)
        )

        assertTrue(tokenResult is AppResult.Success)
        val accessToken = (tokenResult as AppResult.Success).data

        val verifyResult = provider.verifyAccessToken(accessToken)

        assertTrue(verifyResult is AppResult.Success)
        assertEquals(userId, (verifyResult as AppResult.Success).data)
    }

    @Test
    fun `verifyAccessToken returns expired error for expired token`() {
        val userId = UserId.generate()
        val sessionId = UserSessionId.generate()
        val now = Instant.now()

        val tokenResult = provider.generateAccessToken(
            userId = userId,
            userRole = UserRole.USER,
            sessionId = sessionId,
            issuedAt = now.minusSeconds(120),
            expiration = now.minusSeconds(60)
        )

        assertTrue(tokenResult is AppResult.Success)
        val accessToken = (tokenResult as AppResult.Success).data

        val verifyResult = provider.verifyAccessToken(accessToken)

        assertTrue(verifyResult is AppResult.Error)
        assertTrue((verifyResult as AppResult.Error).error is UserError.AccessTokenExpired)
    }

    @Test
    fun `verifyAccessToken returns invalid error for malformed token`() {
        val verifyResult = provider.verifyAccessToken(AccessToken("not-a-jwt"))

        assertTrue(verifyResult is AppResult.Error)
        assertTrue((verifyResult as AppResult.Error).error is UserError.InvalidAccessToken)
    }

    private fun userConfig(jwtSecret: String): UserConfig {
        return UserConfig(
            jwtSecret = jwtSecret,
            authRealm = "test",
            adminAccountsList = emptyList(),
            managementAuthSettings = ManagementAuthSettings(
                availableAuthProviders = AvailableAuthProviders(
                    primary = listOf(UserAuthProvider.EMAIL),
                    secondary = emptyList()
                ),
                accessTokenValidityHours = 1,
                refreshTokenValidityDays = 30
            ),
            googleWebClientId = null,
            uniOneConfig = null,
            resendConfig = null
        )
    }

    private companion object {
        // 32+ bytes for HMAC-SHA key
        const val SECRET = "01234567890123456789012345678901"
    }
}
