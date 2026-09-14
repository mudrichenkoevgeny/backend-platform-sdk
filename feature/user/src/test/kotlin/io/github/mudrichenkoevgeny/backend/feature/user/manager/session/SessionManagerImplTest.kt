package io.github.mudrichenkoevgeny.backend.feature.user.manager.session

import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.database.manager.redis.RedisManager
import io.github.mudrichenkoevgeny.backend.feature.user.database.repository.user.UserRepository
import io.github.mudrichenkoevgeny.backend.feature.user.database.repository.usersession.UserSessionRepository
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.client.createTestClientInfo
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.session.createTestUserSession
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.session.createTestUserSessionInternal
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.token.RotatedRefreshTokenData
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.provider.authsettings.AuthSettingsProvider
import io.github.mudrichenkoevgeny.backend.feature.user.security.refreshtokenprovider.RefreshTokenProvider
import io.github.mudrichenkoevgeny.backend.feature.user.security.tokenprovider.TokenProvider
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.SortOrder
import io.github.mudrichenkoevgeny.shared.foundation.core.common.serialization.FoundationJson
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserSortValues
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.token.AccessToken
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.token.RefreshToken
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.token.RefreshTokenHash
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.token.SessionToken
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class SessionManagerImplExtendedTest {

    private val authSettingsProvider = mockk<AuthSettingsProvider>()
    private val jwtTokenProvider = mockk<TokenProvider>()
    private val refreshTokenProvider = mockk<RefreshTokenProvider>()
    private val userManager = mockk<UserManager>()
    private val repository = mockk<UserSessionRepository>()
    private val redisManager = mockk<RedisManager>(relaxed = true)
    private val auditLogger = mockk<AuditLogger>(relaxed = true)
    private val userRepository = mockk<UserRepository>(relaxed = true)

    private val manager = SessionManagerImpl(
        authSettingsProvider = authSettingsProvider,
        jwtTokenProvider = jwtTokenProvider,
        refreshTokenProvider = refreshTokenProvider,
        userManager = userManager,
        userSessionRepository = repository,
        redisManager = redisManager,
        auditLogger = auditLogger,
        userRepository = userRepository
    )

    private val userId = UserId.generate()
    private val sessionId = UserSessionId.generate()
    private val clientInfo = createTestClientInfo()

    @Test
    fun `refreshSession with valid session rotates token and saves rotated token in Redis`() = runTest {
        val oldRefreshToken = RefreshToken("old-refresh-token")
        val oldHash = RefreshTokenHash("old-hash")
        val newAccessToken = AccessToken("new-access-token")
        val newRefreshToken = RefreshToken("new-refresh-token")
        val newHash = RefreshTokenHash("new-hash")
        val now = Clock.System.now()
        val expiresAt = now + 3600.seconds

        val currentSession = createSampleInternalSession(userId, oldHash)

        coEvery { refreshTokenProvider.getRefreshTokenHash(oldRefreshToken) } returns AppResult.Success(oldHash)
        coEvery { repository.getUserSessionByHash(oldHash) } returns AppResult.Success(currentSession)
        coEvery { repository.deleteUserSessionById(currentSession.id) } returns AppResult.Success(Unit)

        coEvery { authSettingsProvider.getAccessTokenExpirationSeconds() } returns 900
        coEvery { authSettingsProvider.getRefreshTokenExpirationSeconds() } returns 86400
        coEvery { jwtTokenProvider.generateAccessToken(any(), any(), any(), any(), any()) } returns AppResult.Success(newAccessToken)
        coEvery { refreshTokenProvider.getRefreshToken() } returns AppResult.Success(newRefreshToken)
        coEvery { refreshTokenProvider.getRefreshTokenHash(newRefreshToken) } returns AppResult.Success(newHash)
        coEvery { repository.createUserSession(any()) } returns AppResult.Success(currentSession.copy(expiresAt = expiresAt))
        coEvery { redisManager.setWithExpiration(any(), any(), any()) } returns AppResult.Success(Unit)

        val result = manager.refreshSession(oldRefreshToken, clientInfo)

        assertTrue(result is AppResult.Success)
        val token = (result as AppResult.Success).data
        assertEquals(newAccessToken, token.accessToken)
        assertEquals(newRefreshToken, token.refreshToken)

        coVerify {
            redisManager.setWithExpiration(
                key = "auth:rotated_refresh:old-hash",
                value = any(),
                expirationSeconds = 86400L
            )
        }
    }

    @Test
    fun `refreshSession with rotated token within 30s grace period returns cached session token`() = runTest {
        val oldRefreshToken = RefreshToken("old-refresh-token")
        val oldHash = RefreshTokenHash("old-hash")
        val cachedSessionToken = SessionToken(
            accessToken = AccessToken("cached-access"),
            refreshToken = RefreshToken("cached-refresh"),
            expiresAt = Clock.System.now() + 3600.seconds
        )
        val rotatedData = RotatedRefreshTokenData.from(
            sessionToken = cachedSessionToken,
            userId = userId,
            rotatedAt = Clock.System.now() - 10.seconds
        )

        coEvery { refreshTokenProvider.getRefreshTokenHash(oldRefreshToken) } returns AppResult.Success(oldHash)
        coEvery { repository.getUserSessionByHash(oldHash) } returns AppResult.Success(null)
        coEvery { redisManager.get("auth:rotated_refresh:old-hash") } returns AppResult.Success(FoundationJson.encodeToString(rotatedData))

        val result = manager.refreshSession(oldRefreshToken, clientInfo)

        assertTrue(result is AppResult.Success)
        val token = (result as AppResult.Success).data
        assertEquals(cachedSessionToken.accessToken, token.accessToken)
        assertEquals(cachedSessionToken.refreshToken, token.refreshToken)
    }

    @Test
    fun `refreshSession with rotated token after 30s grace period revokes sessions, sets SECURITY_HOLD, deletes key, and logs audit`() = runTest {
        val oldRefreshToken = RefreshToken("old-refresh-token")
        val oldHash = RefreshTokenHash("old-hash")
        val cachedSessionToken = SessionToken(
            accessToken = AccessToken("cached-access"),
            refreshToken = RefreshToken("cached-refresh"),
            expiresAt = Clock.System.now() + 3600.seconds
        )
        val rotatedData = RotatedRefreshTokenData.from(
            sessionToken = cachedSessionToken,
            userId = userId,
            rotatedAt = Clock.System.now() - 2.minutes
        )

        coEvery { refreshTokenProvider.getRefreshTokenHash(oldRefreshToken) } returns AppResult.Success(oldHash)
        coEvery { repository.getUserSessionByHash(oldHash) } returns AppResult.Success(null)
        coEvery { redisManager.get("auth:rotated_refresh:old-hash") } returns AppResult.Success(FoundationJson.encodeToString(rotatedData))
        coEvery { repository.deleteAllUserSessions(userId) } returns AppResult.Success(Unit)
        coEvery { userRepository.updateUser(userId, status = any()) } returns AppResult.Success(mockk())
        coEvery { redisManager.delete("auth:rotated_refresh:old-hash") } returns AppResult.Success(Unit)

        val result = manager.refreshSession(oldRefreshToken, clientInfo)

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is UserError.InvalidRefreshToken)

        coVerify { repository.deleteAllUserSessions(userId) }
        coVerify { userRepository.updateUser(userId, status = any()) }
        coVerify { redisManager.delete("auth:rotated_refresh:old-hash") }
        verify {
            auditLogger.log(
                actorId = userId.asHexDashString(),
                actorType = any(),
                action = any(),
                resource = any(),
                resourceId = any(),
                status = any(),
                metadata = any()
            )
        }
    }

    @Test
    fun `refreshSession with unknown token returns InvalidRefreshToken error`() = runTest {
        val oldRefreshToken = RefreshToken("unknown-refresh-token")
        val oldHash = RefreshTokenHash("unknown-hash")

        coEvery { refreshTokenProvider.getRefreshTokenHash(oldRefreshToken) } returns AppResult.Success(oldHash)
        coEvery { repository.getUserSessionByHash(oldHash) } returns AppResult.Success(null)
        coEvery { redisManager.get("auth:rotated_refresh:unknown-hash") } returns AppResult.Success(null)

        val result = manager.refreshSession(oldRefreshToken, clientInfo)

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is UserError.InvalidRefreshToken)
    }

    @Test
    fun `updateLastReauthenticated calls repository`() = runTest {
        coEvery { repository.updateLastReauthenticated(sessionId) } returns AppResult.Success(Unit)

        val result = manager.updateLastReauthenticated(sessionId)

        assertTrue(result is AppResult.Success)
    }

    @Test
    fun `deleteAllUserSessions calls repository`() = runTest {
        coEvery { repository.deleteAllUserSessions(userId) } returns AppResult.Success(Unit)

        val result = manager.deleteAllUserSessions(userId)

        assertTrue(result is AppResult.Success)
    }

    @Test
    fun `deleteAllSessionsExceptOneForSelf returns list of deleted ids`() = runTest {
        val deletedIds = listOf(UserSessionId.generate(), UserSessionId.generate())
        coEvery {
            repository.deleteAllUserSessionsExceptOne(userId, sessionId)
        } returns AppResult.Success(deletedIds)

        val result = manager.deleteAllSessionsExceptOneForSelf(userId, sessionId)

        assertTrue(result is AppResult.Success)
        assertEquals(deletedIds, (result as AppResult.Success).data.deletedSessionIds)
    }

    @Test
    fun `getUserSessionsByIdentifierId returns list of internal sessions`() = runTest {
        val identifierId = UserIdentifierId.generate()
        val sessions = listOf(
            createSampleInternalSession(userId, RefreshTokenHash("hash-1")),
            createSampleInternalSession(userId, RefreshTokenHash("hash-2"))
        )

        coEvery {
            repository.getUserSessionsByIdentifierId(identifierId, userId)
        } returns AppResult.Success(sessions)

        val result = manager.getUserSessionsByIdentifierId(identifierId, userId)

        assertTrue(result is AppResult.Success)
        assertEquals(2, (result as AppResult.Success).data.size)
    }

    @Test
    fun `getSessionsPageForSelf returns paged sessions for current user`() = runTest {
        val session = createSampleUserSession(sessionId, userId)
        val paged = PagedResult(listOf(session), 1, 1, 10, 1)

        coEvery {
            repository.getUserSessionsPageByUserId(
                userId = userId,
                pageParams = any(),
                sortBy = any(),
                sortOrder = any(),
                identifiers = any(),
                identifierIds = any(),
                identifierAuthProviders = any(),
                clientTypes = any(),
                userAgents = any(),
                ipAddresses = any(),
                languages = any(),
                deviceIds = any(),
                deviceNames = any(),
                appVersions = any(),
                operationSystemVersions = any()
            )
        } returns AppResult.Success(paged)

        val result = manager.getSessionsPageForSelf(
            userId = userId,
            pageParams = PageParams(1, 10),
            sortBy = UserSortValues.UserSessionSortBy.CREATED_AT,
            sortOrder = SortOrder.DESC,
            identifiers = emptyList(),
            identifierIds = emptyList(),
            identifierAuthProviders = emptyList(),
            clientTypes = emptyList(),
            userAgents = emptyList(),
            ipAddresses = emptyList(),
            languages = emptyList(),
            deviceIds = emptyList(),
            deviceNames = emptyList(),
            appVersions = emptyList(),
            operationSystemVersions = emptyList()
        )

        assertTrue(result is AppResult.Success)
        assertEquals(1, (result as AppResult.Success).data.items.size)
        assertEquals(sessionId, result.data.items.first().id)
    }

    @Test
    fun `deleteLeastRecentlyUsedUserSession returns deleted session id`() = runTest {
        val deletedId = UserSessionId.generate()
        coEvery { repository.deleteLeastRecentlyUsedUserSession(userId) } returns AppResult.Success(deletedId)

        val result = manager.deleteLeastRecentlyUsedUserSession(userId)

        assertTrue(result is AppResult.Success)
        assertEquals(deletedId, (result as AppResult.Success).data)
    }

    private fun createSampleInternalSession(uId: UserId, hash: RefreshTokenHash) = createTestUserSessionInternal(
        userId = uId,
        refreshTokenHash = hash
    )

    private fun createSampleUserSession(sId: UserSessionId, uId: UserId) = createTestUserSession(
        id = sId,
        userId = uId
    )
}
