package io.github.mudrichenkoevgeny.backend.feature.user.manager.session

import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.database.repository.usersession.UserSessionRepository
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.provider.authsettings.AuthSettingsProvider
import io.github.mudrichenkoevgeny.backend.feature.user.security.refreshtokenprovider.RefreshTokenProvider
import io.github.mudrichenkoevgeny.backend.feature.user.security.tokenprovider.TokenProvider
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientDeviceId
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientDeviceInfo
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.SortOrder
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserSortValues
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSession
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionInternal
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.token.RefreshTokenHash
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

class SessionManagerImplExtendedTest {

    private val authSettingsProvider = mockk<AuthSettingsProvider>()
    private val jwtTokenProvider = mockk<TokenProvider>()
    private val refreshTokenProvider = mockk<RefreshTokenProvider>()
    private val userManager = mockk<UserManager>()
    private val repository = mockk<UserSessionRepository>()

    private val manager = SessionManagerImpl(
        authSettingsProvider,
        jwtTokenProvider,
        refreshTokenProvider,
        userManager,
        repository
    )

    private val userId = UserId.generate()
    private val sessionId = UserSessionId.generate()
    private val deviceInfo = ClientDeviceInfo(deviceId = ClientDeviceId.generate())

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

    private fun createSampleInternalSession(uId: UserId, hash: RefreshTokenHash) = UserSessionInternal(
        id = UserSessionId.generate(),
        userId = uId,
        userRole = UserRole.USER,
        identifier = "test",
        identifierId = UserIdentifierId.generate(),
        identifierAuthProvider = UserAuthProvider.EMAIL,
        refreshTokenHash = hash,
        deviceInfo = deviceInfo,
        userAgent = "Mozilla",
        ipAddress = "127.0.0.1",
        expiresAt = Clock.System.now() + 1.days,
        lastAccessedAt = Clock.System.now(),
        lastReauthenticatedAt = Clock.System.now(),
        createdAt = Clock.System.now(),
        updatedAt = null
    )

    private fun createSampleUserSession(sId: UserSessionId, uId: UserId) = UserSession(
        id = sId,
        userId = uId,
        userRole = UserRole.USER,
        identifier = "test",
        identifierId = UserIdentifierId.generate(),
        identifierAuthProvider = UserAuthProvider.EMAIL,
        deviceInfo = deviceInfo,
        userAgent = "Mozilla",
        ipAddress = "127.0.0.1",
        expiresAt = Clock.System.now() + 1.days,
        lastAccessedAt = Clock.System.now(),
        lastReauthenticatedAt = Clock.System.now(),
        isSensitiveValuesMasked = false,
        createdAt = Clock.System.now(),
        updatedAt = null
    )
}