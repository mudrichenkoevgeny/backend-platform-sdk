package io.github.mudrichenkoevgeny.backend.feature.user.database.repository.usersession

import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.util.createTestDataSource
import io.github.mudrichenkoevgeny.backend.feature.user.database.table.UserIdentifiersTable
import io.github.mudrichenkoevgeny.backend.feature.user.database.table.UserSessionsTable
import io.github.mudrichenkoevgeny.backend.feature.user.database.table.UsersTable
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.UserRoleAccessFilter
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientDeviceInfo
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientType
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.SortOrder
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserSortValues
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionInternal
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.token.RefreshTokenHash
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.Instant as JavaInstant
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserSessionRepositoryImplTest {

    private val dataSource = createTestDataSource("user_session_repo")
    private lateinit var repository: UserSessionRepository

    @BeforeAll
    fun setup() {
        Database.connect(dataSource)
        runBlocking {
            suspendTransaction {
                SchemaUtils.drop(UserSessionsTable, UserIdentifiersTable, UsersTable)
                SchemaUtils.create(UsersTable, UserIdentifiersTable, UserSessionsTable)
            }
        }
        repository = UserSessionRepositoryImpl()
    }

    @Test
    fun `createUserSession persists and getUserSessionInternalById returns it`() = runBlocking {
        val userId = createTestUserInDb()
        val identifierId = createTestIdentifierInDb(userId)
        val session = createTestSession(userId, identifierId = identifierId)

        suspendTransaction { repository.createUserSession(session) }
        val result = suspendTransaction { repository.getUserSessionInternalById(session.id) }

        val success = result as AppResult.Success
        assertNotNull(success.data)
        assertEquals(session.id, success.data!!.id)
        assertEquals(session.refreshTokenHash, success.data!!.refreshTokenHash)
    }

    @Test
    fun `deleteAllUserSessionsExceptOne keeps only the specified session`() = runBlocking {
        val userId = createTestUserInDb()
        val identifierId = createTestIdentifierInDb(userId)
        val keepSession = createTestSession(userId, UserSessionId.generate(), identifierId)
        val deleteSession = createTestSession(userId, UserSessionId.generate(), identifierId)

        suspendTransaction {
            repository.createUserSession(keepSession)
            repository.createUserSession(deleteSession)
        }

        val deletedIdsResult = suspendTransaction {
            repository.deleteAllUserSessionsExceptOne(userId, keepSession.id)
        }

        val deletedIds = (deletedIdsResult as AppResult.Success).data
        assertEquals(1, deletedIds.size)
        assertEquals(deleteSession.id, deletedIds.first())

        val checkKeep = suspendTransaction { repository.getUserSessionById(keepSession.id) }
        val checkDelete = suspendTransaction { repository.getUserSessionById(deleteSession.id) }

        assertNotNull((checkKeep as AppResult.Success).data)
        assertNull((checkDelete as AppResult.Success).data)
    }

    @Test
    fun `deleteLeastRecentlyUsedUserSession removes the oldest session`() = runBlocking {
        val userId = createTestUserInDb()
        val identifierId = createTestIdentifierInDb(userId)
        val now = Instant.parse(BASE_TIME)
        val oldSession = createTestSession(userId, UserSessionId.generate(), identifierId)
            .copy(lastAccessedAt = now - 10.days)
        val newSession = createTestSession(userId, UserSessionId.generate(), identifierId)
            .copy(lastAccessedAt = now)

        suspendTransaction {
            repository.createUserSession(oldSession)
            repository.createUserSession(newSession)
        }

        val result = suspendTransaction { repository.deleteLeastRecentlyUsedUserSession(userId) }
        assertEquals(oldSession.id, (result as AppResult.Success).data)

        val sessions = suspendTransaction { repository.getAllUserSessions(userId) }
        val list = (sessions as AppResult.Success).data
        assertEquals(1, list.size)
        assertEquals(newSession.id, list.first().id)
    }

    @Test
    fun `updateLastAccessed updates the timestamp to current`() = runBlocking {
        val userId = createTestUserInDb()
        val identifierId = createTestIdentifierInDb(userId)
        val session = createTestSession(userId, identifierId = identifierId)
        suspendTransaction { repository.createUserSession(session) }

        suspendTransaction { repository.updateLastAccessed(session.id) }
        val updated = suspendTransaction { repository.getUserSessionInternalById(session.id) }

        val data = (updated as AppResult.Success).data!!
        assertTrue(data.lastAccessedAt > session.lastAccessedAt)
    }

    @Test
    fun `getUserSessionByHash finds session by refresh token hash`() = runBlocking {
        val userId = createTestUserInDb()
        val identifierId = createTestIdentifierInDb(userId)
        val hash = RefreshTokenHash("secret_hash_123")
        val session = createTestSession(userId, identifierId = identifierId).copy(refreshTokenHash = hash)
        suspendTransaction { repository.createUserSession(session) }

        val result = suspendTransaction { repository.getUserSessionByHash(hash) }
        assertEquals(session.id, (result as AppResult.Success).data?.id)
    }

    @Test
    fun `getUserSessionsPageByUserId filters by client info fields`() = runBlocking {
        val userId = createTestUserInDb()
        val identifierId = createTestIdentifierInDb(userId)
        val session = createTestSession(userId, identifierId = identifierId).copy(
            deviceInfo = ClientDeviceInfo(
                deviceId = null,
                deviceName = "iPhone 15",
                clientType = ClientType.IOS,
                language = "en",
                appVersion = "1.0.0",
                operationSystemVersion = "17.0"
            )
        )
        suspendTransaction { repository.createUserSession(session) }

        val result = suspendTransaction {
            repository.getUserSessionsPageByUserId(
                userId = userId,
                pageParams = PageParams(1, 10),
                sortBy = UserSortValues.UserSessionSortBy.CREATED_AT,
                sortOrder = SortOrder.DESC,
                deviceNames = listOf("iphone")
            )
        }

        val data = (result as AppResult.Success).data
        assertEquals(1, data.items.size)
        assertEquals("iPhone 15", data.items.first().deviceInfo.deviceName)
    }

    @Test
    fun `getUserSessionsPageWithAccessFilter joins with UsersTable for role filtering`() = runBlocking {
        val adminId = createTestUserInDb(UserRole.ADMIN)
        val userId = createTestUserInDb(UserRole.USER)

        val adminIdentifier = createTestIdentifierInDb(adminId)
        val userIdentifier = createTestIdentifierInDb(userId)

        suspendTransaction {
            repository.createUserSession(createTestSession(adminId, identifierId = adminIdentifier))
            repository.createUserSession(createTestSession(userId, identifierId = userIdentifier))
        }

        val result = suspendTransaction {
            repository.getUserSessionsPageWithAccessFilter(
                accessFilter = UserRoleAccessFilter(setOf(UserRole.ADMIN)),
                pageParams = PageParams(1, 10),
                sortBy = UserSortValues.UserSessionSortBy.CREATED_AT,
                sortOrder = SortOrder.ASC,
                userRoles = listOf(UserRole.ADMIN)
            )
        }

        val data = (result as AppResult.Success).data
        assertEquals(1, data.items.size)
        assertEquals(adminId, data.items.first().userId)
    }

    private suspend fun createTestUserInDb(role: UserRole = UserRole.USER): UserId {
        val id = UserId.generate()
        suspendTransaction {
            UsersTable.insert {
                it[UsersTable.id] = id.value
                it[UsersTable.role] = role
                it[accountStatus] = UserAccountStatus.ACTIVE
                it[authorityLevel] = 1
                it[permissionCodes] = emptySet()
                it[isTotpEnabled] = false
                it[createdAt] = JavaInstant.now()
            }
        }
        return id
    }

    private suspend fun createTestIdentifierInDb(userId: UserId): UserIdentifierId {
        val id = UserIdentifierId.generate()
        suspendTransaction {
            UserIdentifiersTable.insert {
                it[UserIdentifiersTable.id] = id.value
                it[UserIdentifiersTable.userId] = userId.value
                it[userAuthProvider] = UserAuthProvider.EMAIL
                it[identifier] = "test@example.com"
                it[createdAt] = JavaInstant.now()
            }
        }
        return id
    }

    private fun createTestSession(
        userId: UserId,
        sessionId: UserSessionId = UserSessionId.generate(),
        identifierId: UserIdentifierId = UserIdentifierId.generate()
    ) = UserSessionInternal(
        id = sessionId,
        userId = userId,
        userRole = UserRole.USER,
        identifier = "test@example.com",
        identifierId = identifierId,
        identifierAuthProvider = UserAuthProvider.EMAIL,
        refreshTokenHash = RefreshTokenHash("hash"),
        deviceInfo = ClientDeviceInfo(
            deviceId = null,
            deviceName = "Test Device",
            clientType = ClientType.WEB,
            language = "en",
            appVersion = "1.0",
            operationSystemVersion = "Win 11"
        ),
        userAgent = "Mozilla/5.0",
        ipAddress = "127.0.0.1",
        expiresAt = Instant.parse(BASE_TIME) + 30.days,
        lastAccessedAt = Instant.parse(BASE_TIME),
        lastReauthenticatedAt = Instant.parse(BASE_TIME),
        createdAt = Instant.parse(BASE_TIME),
        updatedAt = null
    )

    private companion object {
        const val BASE_TIME = "2026-05-04T12:00:00Z"
    }
}