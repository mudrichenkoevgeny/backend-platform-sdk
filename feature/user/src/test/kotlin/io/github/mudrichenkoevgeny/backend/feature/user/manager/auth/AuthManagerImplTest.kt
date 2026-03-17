package io.github.mudrichenkoevgeny.backend.feature.user.manager.auth

import io.github.mudrichenkoevgeny.backend.core.common.model.UserId
import io.github.mudrichenkoevgeny.backend.core.common.network.request.model.ClientInfo
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.useridentifier.UserIdentifierManager
import io.github.mudrichenkoevgeny.backend.feature.user.model.user.User
import io.github.mudrichenkoevgeny.backend.feature.user.model.useridentifier.UserIdentifier
import io.github.mudrichenkoevgeny.backend.feature.user.testutil.ExposedTestDb
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.UserRole
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class AuthManagerImplTest {

    private val userManager: UserManager = mockk()
    private val userIdentifierManager: UserIdentifierManager = mockk()
    private val sessionManager: SessionManager = mockk()

    private val manager = AuthManagerImpl(
        userManager = userManager,
        userIdentifierManager = userIdentifierManager,
        sessionManager = sessionManager
    )

    @Test
    fun `provideAuthData returns UserForbidden when role not allowed`() = runBlocking {
        ExposedTestDb.initOnce()

        val user = User(
            id = USER_ID,
            role = UserRole.ADMIN,
            accountStatus = UserAccountStatus.ACTIVE,
            lastLoginAt = null,
            lastActiveAt = null,
            createdAt = Instant.now(),
            updatedAt = null
        )

        coEvery { userManager.getUserById(USER_ID) } returns AppResult.Success(user)

        val identifier = UserIdentifier(
            id = io.github.mudrichenkoevgeny.backend.core.common.model.UserIdentifierId.generate(),
            userId = USER_ID,
            userAuthProvider = UserAuthProvider.EMAIL,
            identifier = IDENTIFIER,
            passwordHash = null,
            createdAt = Instant.now(),
            updatedAt = null
        )

        val result = manager.provideAuthData(
            userIdentifier = identifier,
            clientInfo = CLIENT_INFO,
            allowedRoles = setOf(UserRole.USER)
        )

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is UserError.UserForbidden)
    }

    @Test
    fun `provideAuthData returns UserBlocked when account banned`() = runBlocking {
        ExposedTestDb.initOnce()

        val user = User(
            id = USER_ID,
            role = UserRole.USER,
            accountStatus = UserAccountStatus.BANNED,
            lastLoginAt = null,
            lastActiveAt = null,
            createdAt = Instant.now(),
            updatedAt = null
        )

        coEvery { userManager.getUserById(USER_ID) } returns AppResult.Success(user)

        val identifier = UserIdentifier(
            id = io.github.mudrichenkoevgeny.backend.core.common.model.UserIdentifierId.generate(),
            userId = USER_ID,
            userAuthProvider = UserAuthProvider.EMAIL,
            identifier = IDENTIFIER,
            passwordHash = null,
            createdAt = Instant.now(),
            updatedAt = null
        )

        val result = manager.provideAuthData(
            userIdentifier = identifier,
            clientInfo = CLIENT_INFO,
            allowedRoles = setOf(UserRole.USER)
        )

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is UserError.UserBlocked)
    }

    @Test
    fun `getOrCreateUserIdentifier returns existing identifier when present`() = runBlocking {
        ExposedTestDb.initOnce()

        val existing = UserIdentifier(
            id = io.github.mudrichenkoevgeny.backend.core.common.model.UserIdentifierId.generate(),
            userId = USER_ID,
            userAuthProvider = UserAuthProvider.EMAIL,
            identifier = IDENTIFIER,
            passwordHash = null,
            createdAt = Instant.now(),
            updatedAt = null
        )

        coEvery {
            userIdentifierManager.getUserIdentifier(
                userAuthProvider = UserAuthProvider.EMAIL,
                identifier = IDENTIFIER
            )
        } returns AppResult.Success(existing)

        val result = manager.getOrCreateUserIdentifier(
            userAuthProvider = UserAuthProvider.EMAIL,
            identifier = IDENTIFIER,
            password = null,
            userId = null,
            userRole = UserRole.USER
        )

        assertTrue(result is AppResult.Success)
    }

    private companion object {
        val USER_ID: UserId = UserId.generate()
        const val IDENTIFIER = "user@example.com"

        val CLIENT_INFO = ClientInfo(
            clientType = null,
            userAgent = null,
            ipAddress = "127.0.0.1",
            language = null,
            host = null,
            origin = null,
            deviceId = null,
            deviceName = null,
            appVersion = null,
            operationSystemVersion = null
        )
    }
}

