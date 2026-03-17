package io.github.mudrichenkoevgeny.backend.feature.user.manager.session

import io.github.mudrichenkoevgeny.backend.core.common.model.UserId
import io.github.mudrichenkoevgeny.backend.core.common.model.UserIdentifierId
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.config.model.UserConfig
import io.github.mudrichenkoevgeny.backend.feature.user.database.repository.usersession.UserSessionRepository
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.model.auth.AccessToken
import io.github.mudrichenkoevgeny.backend.feature.user.model.auth.RefreshToken
import io.github.mudrichenkoevgeny.backend.feature.user.model.auth.RefreshTokenHash
import io.github.mudrichenkoevgeny.backend.feature.user.model.session.UserSession
import io.github.mudrichenkoevgeny.backend.feature.user.security.refreshtokenprovider.RefreshTokenProvider
import io.github.mudrichenkoevgeny.backend.feature.user.security.tokenprovider.TokenProvider
import io.github.mudrichenkoevgeny.backend.feature.user.testutil.ExposedTestDb
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.UserAuthProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import io.github.mudrichenkoevgeny.backend.core.common.network.request.model.ClientInfo

class SessionManagerImplTest {

    private val userConfig: UserConfig = mockk()
    private val tokenProvider: TokenProvider = mockk()
    private val refreshTokenProvider: RefreshTokenProvider = mockk()
    private val repository: UserSessionRepository = mockk()

    private val manager = SessionManagerImpl(
        userConfig = userConfig,
        jwtTokenProvider = tokenProvider,
        refreshTokenProvider = refreshTokenProvider,
        userSessionRepository = repository
    )

    @Test
    fun `createSession returns SessionToken when repository persists session`() = runBlocking {
        ExposedTestDb.initOnce()

        every { userConfig.getAccessTokenValidityHoursDuration() } returns Duration.ofHours(ACCESS_HOURS)
        every { userConfig.getRefreshTokenValidityDaysDuration() } returns Duration.ofDays(REFRESH_DAYS)

        every { tokenProvider.generateAccessToken(any(), any(), any(), any()) } returns AppResult.Success(AccessToken(ACCESS_TOKEN))
        every { refreshTokenProvider.getRefreshToken() } returns AppResult.Success(RefreshToken(REFRESH_TOKEN))
        every { refreshTokenProvider.getRefreshTokenHash(RefreshToken(REFRESH_TOKEN)) } returns AppResult.Success(RefreshTokenHash(REFRESH_HASH))

        val sessionSlot = slot<UserSession>()
        coEvery { repository.createUserSession(capture(sessionSlot)) } answers {
            AppResult.Success(sessionSlot.captured)
        }

        val result = manager.createSession(
            userId = USER_ID,
            userIdentifierId = USER_IDENTIFIER_ID,
            userIdentifierAuthProvider = UserAuthProvider.EMAIL,
            clientInfo = TestClientInfo.DEFAULT,
            lastReauthenticatedAt = Instant.parse(LAST_REAUTH_AT)
        )

        assertTrue(result is AppResult.Success)
        val token = (result as AppResult.Success).data
        assertEquals(ACCESS_TOKEN, token.accessToken.value)
        assertEquals(REFRESH_TOKEN, token.refreshToken.value)
        assertEquals(sessionSlot.captured.expiresAt, token.expiresAt)

        coVerify(exactly = 1) { repository.createUserSession(any()) }
    }

    @Test
    fun `refreshSession returns InvalidRefreshToken when session is missing`() = runBlocking {
        ExposedTestDb.initOnce()

        every { refreshTokenProvider.getRefreshTokenHash(RefreshToken(REFRESH_TOKEN)) } returns AppResult.Success(RefreshTokenHash(REFRESH_HASH))
        coEvery { repository.getUserSessionByHash(userId = null, refreshTokenHash = RefreshTokenHash(REFRESH_HASH)) } returns AppResult.Success(null)

        val result = manager.refreshSession(
            userId = null,
            refreshToken = RefreshToken(REFRESH_TOKEN),
            clientInfo = TestClientInfo.DEFAULT
        )

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is UserError.InvalidRefreshToken)
    }

    private object TestClientInfo {
        val DEFAULT = ClientInfo(
            clientType = null,
            userAgent = null,
            ipAddress = IP_ADDRESS,
            language = null,
            host = null,
            origin = null,
            deviceId = null,
            deviceName = null,
            appVersion = null,
            operationSystemVersion = null
        )
    }

    private companion object {
        val USER_ID: UserId = UserId.generate()
        val USER_IDENTIFIER_ID: UserIdentifierId = UserIdentifierId.generate()

        const val ACCESS_TOKEN = "access"
        const val REFRESH_TOKEN = "refresh"
        const val REFRESH_HASH = "hash"

        const val IP_ADDRESS = "127.0.0.1"
        const val LAST_REAUTH_AT = "2026-01-01T00:00:00Z"

        const val ACCESS_HOURS = 1L
        const val REFRESH_DAYS = 7L
    }
}

