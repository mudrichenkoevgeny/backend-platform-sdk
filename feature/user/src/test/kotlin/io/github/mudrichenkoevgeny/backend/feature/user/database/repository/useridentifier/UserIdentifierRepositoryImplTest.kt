package io.github.mudrichenkoevgeny.backend.feature.user.database.repository.useridentifier

import io.github.mudrichenkoevgeny.backend.core.common.model.UserId
import io.github.mudrichenkoevgeny.backend.core.common.model.UserIdentifierId
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.database.repository.user.UserRepositoryImpl
import io.github.mudrichenkoevgeny.backend.feature.user.database.table.UserIdentifiersTable
import io.github.mudrichenkoevgeny.backend.feature.user.database.table.UsersTable
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

class UserIdentifierRepositoryImplTest {

    private val repository = UserIdentifierRepositoryImpl()

    @BeforeEach
    fun setUp() {
        ExposedTestDb.initOnce()
        ExposedTestDb.dropSchema(UserIdentifiersTable, UsersTable)
        ExposedTestDb.createSchema(UsersTable, UserIdentifiersTable)
    }

    private suspend fun insertUser(userId: UserId): User {
        val user = User(
            id = userId,
            role = UserRole.USER,
            accountStatus = UserAccountStatus.ACTIVE,
            lastLoginAt = null,
            lastActiveAt = null,
            createdAt = Instant.now(),
            updatedAt = null
        )
        val userRepo = UserRepositoryImpl()
        ExposedTestDb.tx { userRepo.createUser(user) }
        return user
    }

    @Test
    fun `createUserIdentifier persists and returns success`() = runBlocking {
        val userId = UserId.generate()
        insertUser(userId)
        val identifier = UserIdentifier(
            id = UserIdentifierId.generate(),
            userId = userId,
            userAuthProvider = UserAuthProvider.EMAIL,
            identifier = IDENTIFIER_EMAIL,
            passwordHash = PASSWORD_HASH,
            createdAt = Instant.now(),
            updatedAt = null
        )

        val result = ExposedTestDb.tx { repository.createUserIdentifier(identifier) }

        assertTrue(result is AppResult.Success)
        assertEquals(identifier, (result as AppResult.Success).data)
    }

    @Test
    fun `getUserIdentifierById returns identifier when found`() = runBlocking {
        val userId = UserId.generate()
        insertUser(userId)
        val createdAt = Instant.parse(CREATED_AT)
        val identifier = UserIdentifier(
            id = UserIdentifierId.generate(),
            userId = userId,
            userAuthProvider = UserAuthProvider.PHONE,
            identifier = IDENTIFIER_PHONE,
            passwordHash = null,
            createdAt = createdAt,
            updatedAt = null
        )
        ExposedTestDb.tx { repository.createUserIdentifier(identifier) }

        val result = ExposedTestDb.tx { repository.getUserIdentifierById(identifier.id) }

        assertTrue(result is AppResult.Success)
        assertEquals(identifier, (result as AppResult.Success).data)
    }

    @Test
    fun `getUserIdentifier returns by provider and identifier value`() = runBlocking {
        val userId = UserId.generate()
        insertUser(userId)
        val createdAt = Instant.parse(CREATED_AT)
        val identifier = UserIdentifier(
            id = UserIdentifierId.generate(),
            userId = userId,
            userAuthProvider = UserAuthProvider.EMAIL,
            identifier = IDENTIFIER_EMAIL,
            passwordHash = null,
            createdAt = createdAt,
            updatedAt = null
        )
        ExposedTestDb.tx { repository.createUserIdentifier(identifier) }

        val result = ExposedTestDb.tx {
            repository.getUserIdentifier(
                userAuthProvider = UserAuthProvider.EMAIL,
                identifier = IDENTIFIER_EMAIL
            )
        }

        assertTrue(result is AppResult.Success)
        assertEquals(identifier, (result as AppResult.Success).data)
    }

    @Test
    fun `getUserIdentifiersListByUserId returns list for user`() = runBlocking {
        val userId = UserId.generate()
        insertUser(userId)
        val id1 = UserIdentifier(
            id = UserIdentifierId.generate(),
            userId = userId,
            userAuthProvider = UserAuthProvider.EMAIL,
            identifier = IDENTIFIER_EMAIL,
            passwordHash = null,
            createdAt = Instant.now(),
            updatedAt = null
        )
        val id2 = UserIdentifier(
            id = UserIdentifierId.generate(),
            userId = userId,
            userAuthProvider = UserAuthProvider.PHONE,
            identifier = IDENTIFIER_PHONE,
            passwordHash = null,
            createdAt = Instant.now(),
            updatedAt = null
        )
        ExposedTestDb.tx { repository.createUserIdentifier(id1) }
        ExposedTestDb.tx { repository.createUserIdentifier(id2) }

        val result = ExposedTestDb.tx { repository.getUserIdentifiersListByUserId(userId) }

        assertTrue(result is AppResult.Success)
        assertEquals(2, (result as AppResult.Success).data.size)
    }

    @Test
    fun `updateUserIdentifier updates identifier and passwordHash`() = runBlocking {
        val userId = UserId.generate()
        insertUser(userId)
        val identifier = UserIdentifier(
            id = UserIdentifierId.generate(),
            userId = userId,
            userAuthProvider = UserAuthProvider.EMAIL,
            identifier = IDENTIFIER_EMAIL,
            passwordHash = null,
            createdAt = Instant.now(),
            updatedAt = null
        )
        ExposedTestDb.tx { repository.createUserIdentifier(identifier) }

        val result = ExposedTestDb.tx {
            repository.updateUserIdentifier(
                userIdentifier = identifier,
                identifier = IDENTIFIER_UPDATED,
                passwordHash = PASSWORD_HASH_UPDATED
            )
        }

        assertTrue(result is AppResult.Success)
        val updated = (result as AppResult.Success).data
        assertEquals(IDENTIFIER_UPDATED, updated.identifier)
        assertEquals(PASSWORD_HASH_UPDATED, updated.passwordHash)
    }

    @Test
    fun `deleteUserIdentifier removes identifier`() = runBlocking {
        val userId = UserId.generate()
        insertUser(userId)
        val identifier = UserIdentifier(
            id = UserIdentifierId.generate(),
            userId = userId,
            userAuthProvider = UserAuthProvider.EMAIL,
            identifier = IDENTIFIER_EMAIL,
            passwordHash = null,
            createdAt = Instant.now(),
            updatedAt = null
        )
        ExposedTestDb.tx { repository.createUserIdentifier(identifier) }

        val deleteResult = ExposedTestDb.tx { repository.deleteUserIdentifier(identifier.id) }
        assertTrue(deleteResult is AppResult.Success)

        val getResult = ExposedTestDb.tx { repository.getUserIdentifierById(identifier.id) }
        assertTrue(getResult is AppResult.Success)
        assertNull((getResult as AppResult.Success).data)
    }

    private companion object {
        const val IDENTIFIER_EMAIL = "user@example.com"
        const val IDENTIFIER_PHONE = "+10000000000"
        const val IDENTIFIER_UPDATED = "updated@example.com"
        const val PASSWORD_HASH = "hash"
        const val PASSWORD_HASH_UPDATED = "hash2"
        const val CREATED_AT = "2026-01-01T00:00:00Z"
    }
}
