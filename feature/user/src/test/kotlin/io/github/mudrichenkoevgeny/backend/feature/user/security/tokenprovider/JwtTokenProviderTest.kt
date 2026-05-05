package io.github.mudrichenkoevgeny.backend.feature.user.security.tokenprovider

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.config.model.UserConfig
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.security.refreshtokenprovider.RefreshTokenProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.token.AccessToken
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.token.RefreshToken
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.token.RefreshTokenHash
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.hours

class JwtTokenProviderTest {

    private val refreshTokenProvider = mockk<RefreshTokenProvider>()
    private val userConfig = mockk<UserConfig>()

    private lateinit var provider: JwtTokenProvider

    @BeforeEach
    fun setUp() {
        every { userConfig.jwtSecret } returns SECRET
        provider = JwtTokenProvider(userConfig, refreshTokenProvider)
    }

    @Test
    fun `should generate and verify valid access token`() {
        val userId = UserId.generate()
        val sessionId = UserSessionId.generate()
        val now = kotlin.time.TimeSource.Monotonic.markNow().plus(0.hours).let {
            kotlin.time.Clock.System.now()
        }
        val expiry = now + 1.hours

        val tokenResult = provider.generateAccessToken(
            userId = userId,
            userRole = UserRole.ADMIN,
            sessionId = sessionId,
            issuedAt = now,
            expiration = expiry
        )

        assertTrue(tokenResult is AppResult.Success)
        val token = (tokenResult as AppResult.Success).data

        val verifyResult = provider.verifyAccessToken(token)
        assertTrue(verifyResult is AppResult.Success)
        assertEquals(userId, (verifyResult as AppResult.Success).data)
    }

    @Test
    fun `should return error for expired token`() {
        val userId = UserId.generate()
        val now = kotlin.time.Clock.System.now()
        val pastIssuedAt = now - 2.hours
        val pastExpiry = now - 1.hours

        val tokenResult = provider.generateAccessToken(
            userId = userId,
            userRole = UserRole.USER,
            sessionId = UserSessionId.generate(),
            issuedAt = pastIssuedAt,
            expiration = pastExpiry
        )

        val token = (tokenResult as AppResult.Success).data
        val verifyResult = provider.verifyAccessToken(token)

        assertTrue(verifyResult is AppResult.Error)
        assertTrue((verifyResult as AppResult.Error).error is UserError.AccessTokenExpired)
    }

    @Test
    fun `should return error for malformed token`() {
        val verifyResult = provider.verifyAccessToken(AccessToken("invalid.payload.signature"))

        assertTrue(verifyResult is AppResult.Error)
        assertTrue((verifyResult as AppResult.Error).error is UserError.InvalidAccessToken)
    }

    @Test
    fun `should delegate refresh token operations to provider`() {
        val expectedToken = RefreshToken("ref-token")
        val expectedHash = RefreshTokenHash("ref-hash")

        every { refreshTokenProvider.getRefreshToken() } returns AppResult.Success(expectedToken)
        every { refreshTokenProvider.getRefreshTokenHash(expectedToken) } returns AppResult.Success(expectedHash)

        val genResult = provider.generateRefreshToken()
        val hashResult = provider.getRefreshTokenHash(expectedToken)

        assertEquals(expectedToken, (genResult as AppResult.Success).data)
        assertEquals(expectedHash, (hashResult as AppResult.Success).data)

        verify { refreshTokenProvider.getRefreshToken() }
        verify { refreshTokenProvider.getRefreshTokenHash(expectedToken) }
    }

    private companion object {
        const val SECRET = "01234567890123456789012345678901"
    }
}