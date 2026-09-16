package io.github.mudrichenkoevgeny.backend.feature.user.manager.auth

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.lockout.LockoutManager
import io.github.mudrichenkoevgeny.backend.core.security.passwordhasher.PasswordHasher
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.auth.createTestSessionToken
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.identifier.createTestUserIdentifierInternal
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.user.createTestUserDetails
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.identifier.IdentifierManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.manager.WebSocketManager
import io.github.mudrichenkoevgeny.backend.feature.user.provider.authsettings.AuthSettingsProvider
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.session.createTestUserSessionInternal
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientInfo
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.v1.jdbc.Database
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AuthManagerImplTest {

    private companion object {
        private const val TEST_EMAIL = "test@test.com"
        private const val TEST_PASSWORD = "password"
    }

    private val userManager = mockk<UserManager>()
    private val identifierManager = mockk<IdentifierManager>()
    private val sessionManager = mockk<SessionManager>()
    private val passwordHasher = mockk<PasswordHasher>()
    private val authSettingsProvider = mockk<AuthSettingsProvider>()
    private val webSocketManager = mockk<WebSocketManager>()
    private val lockoutManager = mockk<LockoutManager>()

    private val authManager = AuthManagerImpl(
        userManager,
        identifierManager,
        sessionManager,
        passwordHasher,
        authSettingsProvider,
        webSocketManager,
        lockoutManager
    )

    @BeforeEach
    fun setup() {
        Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;", driver = "org.h2.Driver")
        coEvery { authSettingsProvider.getMaxActiveSessionsForOpenUser() } returns 5
        coEvery { authSettingsProvider.getMaxActiveSessionsForManagementUser() } returns 3

        coEvery { lockoutManager.isIndefiniteLockout(any()) } returns AppResult.Success(false)
        coEvery { lockoutManager.getLockoutUntil(any()) } returns AppResult.Success(null)
        coEvery { lockoutManager.clearLockout(any()) } returns AppResult.Success(Unit)
    }

    @Test
    fun `authenticateExistingUser returns valid AuthData with SessionToken`() = runTest {
        val userId = UserId.generate()
        val clientInfo = mockk<ClientInfo>(relaxed = true)
        val userDetails = createTestUserDetails(id = userId, role = UserRole.USER)
        val userIdentifier = createTestUserIdentifierInternal(userId = userId)
        val expectedToken = createTestSessionToken()

        coEvery {
            identifierManager.getUserIdentifierInternalByProvider(any(), any())
        } returns AppResult.Success(userIdentifier)

        coEvery { userManager.getUserByIdForSelf(userId) } returns AppResult.Success(userDetails)
        coEvery { sessionManager.getAllUserSessions(userId) } returns AppResult.Success(emptyList())

        coEvery { passwordHasher.isPasswordValid(any(), any()) } returns AppResult.Success(true)

        coEvery {
            sessionManager.createSession(userId, any(), any(), any(), any(), any(), any(), any())
        } returns AppResult.Success(expectedToken)

        val result = authManager.authenticateExistingUser(
            clientInfo = clientInfo,
            userAuthProvider = UserAuthProvider.EMAIL,
            identifier = TEST_EMAIL,
            password = TEST_PASSWORD
        )

        assertTrue(result is AppResult.Success)
        val data = (result as AppResult.Success).data
        assertEquals(userId, data.userDetails.id)
        assertEquals(expectedToken.accessToken, data.sessionToken.accessToken)
        assertEquals(expectedToken.refreshToken, data.sessionToken.refreshToken)
    }

    @Test
    fun `provideAuthData returns Error UserBlocked when status is BANNED`() = runTest {
        val userId = UserId.generate()
        val userDetails = createTestUserDetails(id = userId, role = UserRole.USER, accountStatus = UserAccountStatus.BANNED)
        val userIdentifier = createTestUserIdentifierInternal(userId = userId)

        coEvery {
            identifierManager.getUserIdentifierInternalByProvider(any(), any())
        } returns AppResult.Success(userIdentifier)
        coEvery { userManager.getUserByIdForSelf(userId) } returns AppResult.Success(userDetails)

        coEvery { passwordHasher.isPasswordValid(isNull(), any()) } returns AppResult.Success(true)

        val result = authManager.authenticateExistingUser(
            mockk(relaxed = true), UserAuthProvider.GOOGLE, "ext-id", null
        )

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is UserError.UserBlocked)
    }

    @Test
    fun `authenticateExistingUser deletes least recently used session when open user reaches maxActiveSessionsForOpenUser`() = runTest {
        val userId = UserId.generate()
        val clientInfo = mockk<ClientInfo>(relaxed = true)
        val userDetails = createTestUserDetails(id = userId, role = UserRole.USER)
        val userIdentifier = createTestUserIdentifierInternal(userId = userId)
        val expectedToken = createTestSessionToken()
        val existingSessions = List(5) { createTestUserSessionInternal(userId = userId, userRole = UserRole.USER) }
        val oldestSessionId = existingSessions.first().id

        coEvery {
            identifierManager.getUserIdentifierInternalByProvider(any(), any())
        } returns AppResult.Success(userIdentifier)
        coEvery { userManager.getUserByIdForSelf(userId) } returns AppResult.Success(userDetails)
        coEvery { sessionManager.getAllUserSessions(userId) } returns AppResult.Success(existingSessions)
        coEvery { sessionManager.deleteLeastRecentlyUsedUserSession(userId) } returns AppResult.Success(oldestSessionId)
        coEvery { webSocketManager.sendMessageToUserSession(oldestSessionId, any()) } returns Unit
        coEvery { passwordHasher.isPasswordValid(any(), any()) } returns AppResult.Success(true)
        coEvery {
            sessionManager.createSession(userId, any(), any(), any(), any(), any(), any(), any())
        } returns AppResult.Success(expectedToken)

        val result = authManager.authenticateExistingUser(
            clientInfo = clientInfo,
            userAuthProvider = UserAuthProvider.EMAIL,
            identifier = TEST_EMAIL,
            password = TEST_PASSWORD
        )

        assertTrue(result is AppResult.Success)
        coVerify(exactly = 1) { authSettingsProvider.getMaxActiveSessionsForOpenUser() }
        coVerify(exactly = 1) { sessionManager.deleteLeastRecentlyUsedUserSession(userId) }
        coVerify(exactly = 1) { webSocketManager.sendMessageToUserSession(oldestSessionId, any()) }
    }

    @Test
    fun `authenticateExistingUser deletes least recently used session when management user reaches maxActiveSessionsForManagementUser`() = runTest {
        val userId = UserId.generate()
        val clientInfo = mockk<ClientInfo>(relaxed = true)
        val userDetails = createTestUserDetails(id = userId, role = UserRole.ADMIN)
        val userIdentifier = createTestUserIdentifierInternal(userId = userId)
        val expectedToken = createTestSessionToken()
        val existingSessions = List(3) { createTestUserSessionInternal(userId = userId, userRole = UserRole.ADMIN) }
        val oldestSessionId = existingSessions.first().id

        coEvery {
            identifierManager.getUserIdentifierInternalByProvider(any(), any())
        } returns AppResult.Success(userIdentifier)
        coEvery { userManager.getUserByIdForSelf(userId) } returns AppResult.Success(userDetails)
        coEvery { sessionManager.getAllUserSessions(userId) } returns AppResult.Success(existingSessions)
        coEvery { sessionManager.deleteLeastRecentlyUsedUserSession(userId) } returns AppResult.Success(oldestSessionId)
        coEvery { webSocketManager.sendMessageToUserSession(oldestSessionId, any()) } returns Unit
        coEvery { passwordHasher.isPasswordValid(any(), any()) } returns AppResult.Success(true)
        coEvery {
            sessionManager.createSession(userId, any(), any(), any(), any(), any(), any(), any())
        } returns AppResult.Success(expectedToken)

        val result = authManager.authenticateExistingUser(
            clientInfo = clientInfo,
            userAuthProvider = UserAuthProvider.EMAIL,
            identifier = TEST_EMAIL,
            password = TEST_PASSWORD
        )

        assertTrue(result is AppResult.Success)
        coVerify(exactly = 1) { authSettingsProvider.getMaxActiveSessionsForManagementUser() }
        coVerify(exactly = 1) { sessionManager.deleteLeastRecentlyUsedUserSession(userId) }
        coVerify(exactly = 1) { webSocketManager.sendMessageToUserSession(oldestSessionId, any()) }
    }

    @Test
    fun `authenticateExistingUser does not delete session when active sessions count is below limit`() = runTest {
        val userId = UserId.generate()
        val clientInfo = mockk<ClientInfo>(relaxed = true)
        val userDetails = createTestUserDetails(id = userId, role = UserRole.USER)
        val userIdentifier = createTestUserIdentifierInternal(userId = userId)
        val expectedToken = createTestSessionToken()
        val existingSessions = List(2) { createTestUserSessionInternal(userId = userId, userRole = UserRole.USER) }

        coEvery {
            identifierManager.getUserIdentifierInternalByProvider(any(), any())
        } returns AppResult.Success(userIdentifier)
        coEvery { userManager.getUserByIdForSelf(userId) } returns AppResult.Success(userDetails)
        coEvery { sessionManager.getAllUserSessions(userId) } returns AppResult.Success(existingSessions)
        coEvery { passwordHasher.isPasswordValid(any(), any()) } returns AppResult.Success(true)
        coEvery {
            sessionManager.createSession(userId, any(), any(), any(), any(), any(), any(), any())
        } returns AppResult.Success(expectedToken)

        val result = authManager.authenticateExistingUser(
            clientInfo = clientInfo,
            userAuthProvider = UserAuthProvider.EMAIL,
            identifier = TEST_EMAIL,
            password = TEST_PASSWORD
        )

        assertTrue(result is AppResult.Success)
        coVerify(exactly = 0) { sessionManager.deleteLeastRecentlyUsedUserSession(any()) }
        coVerify(exactly = 0) { webSocketManager.sendMessageToUserSession(any(), any()) }
    }
}