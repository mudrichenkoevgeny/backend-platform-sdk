package io.github.mudrichenkoevgeny.backend.feature.user.database.repository.usersession

import io.github.mudrichenkoevgeny.backend.core.common.model.UserId
import io.github.mudrichenkoevgeny.backend.core.common.model.UserIdentifierId
import io.github.mudrichenkoevgeny.backend.core.common.model.UserSessionId
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.database.repository.user.UserRepositoryImpl
import io.github.mudrichenkoevgeny.backend.feature.user.database.repository.useridentifier.UserIdentifierRepositoryImpl
import io.github.mudrichenkoevgeny.backend.feature.user.database.table.UserIdentifiersTable
import io.github.mudrichenkoevgeny.backend.feature.user.database.table.UsersTable
import io.github.mudrichenkoevgeny.backend.feature.user.database.table.UserSessionsTable
import io.github.mudrichenkoevgeny.backend.feature.user.model.auth.RefreshTokenHash
import io.github.mudrichenkoevgeny.backend.feature.user.model.session.UserSession
import io.github.mudrichenkoevgeny.backend.feature.user.model.user.User
import io.github.mudrichenkoevgeny.backend.feature.user.model.useridentifier.UserIdentifier
import io.github.mudrichenkoevgeny.backend.feature.user.testutil.ExposedTestDb
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

class UserSessionRepositoryImplTest {

    private val repository = UserSessionRepositoryImpl()

    @BeforeEach
    fun setUp() {
        ExposedTestDb.initOnce()
        ExposedTestDb.dropSchema(UserSessionsTable, UserIdentifiersTable, UsersTable)
        ExposedTestDb.createSchema(UsersTable, UserIdentifiersTable, UserSessionsTable)
    }

    private suspend fun insertUserAndIdentifier(): Pair<User, UserIdentifier> {
        val userId = UserId.generate()
        val user = User(
            id = userId,
            role = UserRole.USER,
            accountStatus = UserAccountStatus.ACTIVE,
            lastLoginAt = null,
            lastActiveAt = null,
            createdAt = Instant.now(),
            updatedAt = null
        )
        ExposedTestDb.tx { UserRepositoryImpl().createUser(user) }
        val identifier = UserIdentifier(
            id = UserIdentifierId.generate(),
            userId = userId,
            userAuthProvider = UserAuthProvider.EMAIL,
            identifier = IDENTIFIER_EMAIL,
            passwordHash = null,
            createdAt = Instant.now(),
            updatedAt = null
        )
        ExposedTestDb.tx { UserIdentifierRepositoryImpl().createUserIdentifier(identifier) }
        return user to identifier
    }

    @Test
    fun `createUserSession persists and returns success`() = runBlocking {
        val (user, identifier) = insertUserAndIdentifier()
        val now = Instant.parse(CREATED_AT)
        val expiresAt = Instant.parse(EXPIRES_AT)
        val session = UserSession(
            id = UserSessionId.generate(),
            userId = user.id,
            userIdentifierId = identifier.id,
            userIdentifierAuthProvider = identifier.userAuthProvider,
            refreshTokenHash = RefreshTokenHash(TOKEN_HASH),
            expiresAt = expiresAt,
            revoked = false,
            userClientType = null,
            userAgent = null,
            ipAddress = null,
            language = null,
            userDeviceId = null,
            userDeviceName = null,
            appVersion = null,
            operationSystemVersion = null,
            createdAt = now,
            updatedAt = null,
            lastAccessedAt = now,
            lastReauthenticatedAt = now
        )

        val result = ExposedTestDb.tx { repository.createUserSession(session) }

        assertTrue(result is AppResult.Success)
        assertEquals(session, (result as AppResult.Success).data)
    }

    @Test
    fun `getUserSessionById returns session when found`() = runBlocking {
        val (user, identifier) = insertUserAndIdentifier()
        val session = newSession(user.id, identifier.id, identifier.userAuthProvider)
        ExposedTestDb.tx { repository.createUserSession(session) }

        val result = ExposedTestDb.tx { repository.getUserSessionById(session.id) }

        assertTrue(result is AppResult.Success)
        assertEquals(session.id, (result as AppResult.Success).data?.id)
    }

    @Test
    fun `getUserSessionById returns null when not found`() = runBlocking {
        val result = ExposedTestDb.tx { repository.getUserSessionById(UserSessionId.generate()) }

        assertTrue(result is AppResult.Success)
        assertNull((result as AppResult.Success).data)
    }

    @Test
    fun `getAllUserSessions returns list for user`() = runBlocking {
        val (user, identifier) = insertUserAndIdentifier()
        val session1 = newSession(user.id, identifier.id, identifier.userAuthProvider)
        val session2 = newSession(user.id, identifier.id, identifier.userAuthProvider)
        ExposedTestDb.tx { repository.createUserSession(session1) }
        ExposedTestDb.tx { repository.createUserSession(session2) }

        val result = ExposedTestDb.tx { repository.getAllUserSessions(user.id) }

        assertTrue(result is AppResult.Success)
        assertEquals(2, (result as AppResult.Success).data.size)
    }

    @Test
    fun `deleteUserSessionById removes session`() = runBlocking {
        val (user, identifier) = insertUserAndIdentifier()
        val session = newSession(user.id, identifier.id, identifier.userAuthProvider)
        ExposedTestDb.tx { repository.createUserSession(session) }

        val deleteResult = ExposedTestDb.tx { repository.deleteUserSessionById(session.id) }
        assertTrue(deleteResult is AppResult.Success)

        val getResult = ExposedTestDb.tx { repository.getUserSessionById(session.id) }
        assertTrue(getResult is AppResult.Success)
        assertNull((getResult as AppResult.Success).data)
    }

    @Test
    fun `deleteUserSession by userId and refreshTokenHash removes session`() = runBlocking {
        val (user, identifier) = insertUserAndIdentifier()
        val hash = RefreshTokenHash(TOKEN_HASH)
        val session = newSession(user.id, identifier.id, identifier.userAuthProvider, hash)
        ExposedTestDb.tx { repository.createUserSession(session) }

        val deleteResult = ExposedTestDb.tx { repository.deleteUserSession(user.id, hash) }
        assertTrue(deleteResult is AppResult.Success)

        val getResult = ExposedTestDb.tx { repository.getUserSessionById(session.id) }
        assertTrue(getResult is AppResult.Success)
        assertNull((getResult as AppResult.Success).data)
    }

    @Test
    fun `revokeSession sets revoked`() = runBlocking {
        val (user, identifier) = insertUserAndIdentifier()
        val hash = RefreshTokenHash(TOKEN_HASH)
        val session = newSession(user.id, identifier.id, identifier.userAuthProvider, hash)
        ExposedTestDb.tx { repository.createUserSession(session) }

        val revokeResult = ExposedTestDb.tx { repository.revokeSession(hash) }
        assertTrue(revokeResult is AppResult.Success)

        val getResult = ExposedTestDb.tx { repository.getUserSessionByHash(user.id, hash) }
        assertTrue(getResult is AppResult.Success)
        assertTrue((getResult as AppResult.Success).data?.revoked == true)
    }

    private suspend fun newSession(
        userId: UserId,
        userIdentifierId: UserIdentifierId,
        userIdentifierAuthProvider: UserAuthProvider,
        refreshTokenHash: RefreshTokenHash = RefreshTokenHash(TOKEN_HASH)
    ): UserSession {
        val now = Instant.now()
        val expiresAt = now.plusSeconds(3600)
        return UserSession(
            id = UserSessionId.generate(),
            userId = userId,
            userIdentifierId = userIdentifierId,
            userIdentifierAuthProvider = userIdentifierAuthProvider,
            refreshTokenHash = refreshTokenHash,
            expiresAt = expiresAt,
            revoked = false,
            userClientType = null,
            userAgent = null,
            ipAddress = null,
            language = null,
            userDeviceId = null,
            userDeviceName = null,
            appVersion = null,
            operationSystemVersion = null,
            createdAt = now,
            updatedAt = null,
            lastAccessedAt = now,
            lastReauthenticatedAt = now
        )
    }

    private companion object {
        const val IDENTIFIER_EMAIL = "user@example.com"
        const val TOKEN_HASH = "refresh-hash"
        const val CREATED_AT = "2026-01-01T12:00:00Z"
        const val EXPIRES_AT = "2026-01-02T12:00:00Z"
    }
}
