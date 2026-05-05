package io.github.mudrichenkoevgeny.backend.feature.user.database.repository.useridentifier

import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.util.createTestDataSource
import io.github.mudrichenkoevgeny.backend.feature.user.database.table.UserIdentifiersTable
import io.github.mudrichenkoevgeny.backend.feature.user.database.table.UsersTable
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.UserRoleAccessFilter
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.SortOrder
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.passwordhash.PasswordHash
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierInternal
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserSortValues
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
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
import kotlin.time.Instant

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserIdentifierRepositoryImplTest {

    private val dataSource = createTestDataSource("user_identifier_repo")
    private lateinit var repository: UserIdentifierRepository

    @BeforeAll
    fun setup() {
        Database.connect(dataSource)
        runBlocking {
            suspendTransaction {
                SchemaUtils.drop(UserIdentifiersTable, UsersTable)
                SchemaUtils.create(UsersTable, UserIdentifiersTable)
            }
        }
        repository = UserIdentifierRepositoryImpl()
    }

    @Test
    fun `createUserIdentifier persists and getUserIdentifierInternalById returns it`() = runBlocking {
        val userId = createTestUserInDb()
        val identifier = createTestIdentifier(userId)

        suspendTransaction { repository.createUserIdentifier(identifier) }
        val result = suspendTransaction { repository.getUserIdentifierInternalById(identifier.id) }

        val success = result as AppResult.Success
        assertNotNull(success.data)
        assertEquals(identifier.id, success.data!!.id)
        assertEquals(identifier.identifier, success.data!!.identifier)
    }

    @Test
    fun `deleteUserIdentifier removes record and returns error if not exists`() = runBlocking {
        val userId = createTestUserInDb()
        val identifier = createTestIdentifier(userId)
        suspendTransaction { repository.createUserIdentifier(identifier) }

        val deleteResult = suspendTransaction { repository.deleteUserIdentifier(identifier.id) }
        assertTrue(deleteResult is AppResult.Success)

        val checkResult = suspendTransaction { repository.getUserIdentifierInternalById(identifier.id) }
        assertNull((checkResult as AppResult.Success).data)

        val deleteAgain = suspendTransaction { repository.deleteUserIdentifier(identifier.id) }
        assertTrue(deleteAgain is AppResult.Error)
    }

    @Test
    fun `updatePasswordHash updates hash and updatedAt`() = runBlocking {
        val userId = createTestUserInDb()
        val identifier = createTestIdentifier(userId)
        suspendTransaction { repository.createUserIdentifier(identifier) }

        val newHash = PasswordHash("new_secret_hash")
        val result = suspendTransaction { repository.updatePasswordHash(identifier, newHash) }

        val updated = (result as AppResult.Success).data
        assertEquals(newHash, updated.passwordHash)
        assertNotNull(updated.updatedAt)
    }

    @Test
    fun `getUserIdentifiersListByUserId filters by userId and optional provider`() = runBlocking {
        val userId = createTestUserInDb()
        val ident1 = createTestIdentifier(userId, "email1@test.com", UserAuthProvider.EMAIL)
        val ident2 = createTestIdentifier(userId, "google_id", UserAuthProvider.GOOGLE)

        suspendTransaction {
            repository.createUserIdentifier(ident1)
            repository.createUserIdentifier(ident2)
        }

        val allResult = suspendTransaction {
            repository.getUserIdentifiersListByUserId(userId, null)
        }
        assertEquals(2, (allResult as AppResult.Success).data.size)

        val googleOnly = suspendTransaction {
            repository.getUserIdentifiersListByUserId(userId, UserAuthProvider.GOOGLE)
        }
        val googleData = (googleOnly as AppResult.Success).data
        assertEquals(1, googleData.size)
        assertEquals(UserAuthProvider.GOOGLE, googleData.first().userAuthProvider)
    }

    @Test
    fun `getUserIdentifiersPageByUserId filters by identifier substring`() = runBlocking {
        val userId = createTestUserInDb()
        suspendTransaction {
            repository.createUserIdentifier(createTestIdentifier(userId, "apple_user"))
            repository.createUserIdentifier(createTestIdentifier(userId, "banana_user"))
        }

        val result = suspendTransaction {
            repository.getUserIdentifiersPageByUserId(
                userId = userId,
                params = PageParams(1, 10),
                sortBy = UserSortValues.UserIdentifierSortBy.CREATED_AT,
                sortOrder = SortOrder.ASC,
                userAuthProviders = emptyList(),
                identifiers = listOf("PLE")
            )
        }

        val data = (result as AppResult.Success).data
        assertEquals(1, data.items.size)
        assertTrue(data.items.first().identifier.contains("apple"))
    }

    @Test
    fun `getUserIdentifiersPageWithAccessFilter respects user roles`() = runBlocking {
        val adminId = createTestUserInDb(UserRole.ADMIN)
        val userId = createTestUserInDb(UserRole.USER)

        suspendTransaction {
            repository.createUserIdentifier(createTestIdentifier(adminId, "admin_ident"))
            repository.createUserIdentifier(createTestIdentifier(userId, "user_ident"))
        }

        val result = suspendTransaction {
            repository.getUserIdentifiersPageWithAccessFilter(
                accessFilter = UserRoleAccessFilter(setOf(UserRole.ADMIN)),
                params = PageParams(1, 10),
                sortBy = UserSortValues.UserIdentifierSortBy.CREATED_AT,
                sortOrder = SortOrder.ASC,
                userIds = emptyList(),
                userAuthProviders = emptyList(),
                identifiers = emptyList()
            )
        }

        val data = (result as AppResult.Success).data
        assertEquals(1, data.items.size)
        assertEquals("admin_ident", data.items.first().identifier)
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

    private fun createTestIdentifier(
        userId: UserId,
        value: String = "test@example.com",
        provider: UserAuthProvider = UserAuthProvider.EMAIL
    ) = UserIdentifierInternal(
        id = UserIdentifierId.generate(),
        userId = userId,
        userAuthProvider = provider,
        identifier = value,
        externalProviderEmail = null,
        passwordHash = PasswordHash("hash"),
        createdAt = Instant.parse(CREATED_AT),
        updatedAt = null
    )

    private companion object {
        const val CREATED_AT = "2026-01-01T10:00:00Z"
    }
}