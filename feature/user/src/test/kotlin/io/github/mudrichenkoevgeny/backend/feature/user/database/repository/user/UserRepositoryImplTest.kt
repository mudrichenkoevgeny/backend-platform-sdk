package io.github.mudrichenkoevgeny.backend.feature.user.database.repository.user

import io.github.mudrichenkoevgeny.backend.core.common.listing.pagination.model.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.model.UserId
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.database.table.UsersTable
import io.github.mudrichenkoevgeny.backend.feature.user.model.user.User
import io.github.mudrichenkoevgeny.backend.feature.user.testutil.ExposedTestDb
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

class UserRepositoryImplTest {

    private val repository = UserRepositoryImpl()

    @BeforeEach
    fun setUp() {
        ExposedTestDb.initOnce()
        ExposedTestDb.dropSchema(UsersTable)
        ExposedTestDb.createSchema(UsersTable)
    }

    @Test
    fun `createUser persists user and returns success`() = runBlocking {
        val now = Instant.parse(CREATED_AT)
        val user = User(
            id = UserId.generate(),
            role = UserRole.USER,
            accountStatus = UserAccountStatus.ACTIVE,
            lastLoginAt = null,
            lastActiveAt = null,
            createdAt = now,
            updatedAt = null
        )

        val result = ExposedTestDb.tx { repository.createUser(user) }

        assertTrue(result is AppResult.Success)
        assertEquals(user, (result as AppResult.Success).data)
    }

    @Test
    fun `getUserById returns user when found`() = runBlocking {
        val userId = UserId.generate()
        val createdAt = Instant.parse(CREATED_AT)
        val user = User(
            id = userId,
            role = UserRole.ADMIN,
            accountStatus = UserAccountStatus.ACTIVE,
            lastLoginAt = null,
            lastActiveAt = null,
            createdAt = createdAt,
            updatedAt = null
        )
        ExposedTestDb.tx { repository.createUser(user) }

        val result = ExposedTestDb.tx { repository.getUserById(userId) }

        assertTrue(result is AppResult.Success)
        assertEquals(user, (result as AppResult.Success).data)
    }

    @Test
    fun `getUserById returns null when not found`() = runBlocking {
        val result = ExposedTestDb.tx { repository.getUserById(UserId.generate()) }

        assertTrue(result is AppResult.Success)
        assertNull((result as AppResult.Success).data)
    }

    @Test
    fun `updateUser updates accountStatus and lastLoginAt`() = runBlocking {
        val user = User(
            id = UserId.generate(),
            role = UserRole.USER,
            accountStatus = UserAccountStatus.ACTIVE,
            lastLoginAt = null,
            lastActiveAt = null,
            createdAt = Instant.now(),
            updatedAt = null
        )
        ExposedTestDb.tx { repository.createUser(user) }
        val lastLoginAt = Instant.parse(LAST_LOGIN_AT)

        val result = ExposedTestDb.tx {
            repository.updateUser(
                user = user,
                status = UserAccountStatus.BANNED,
                lastLoginAt = lastLoginAt,
                lastActiveAt = null
            )
        }

        assertTrue(result is AppResult.Success)
        val updated = (result as AppResult.Success).data
        assertEquals(UserAccountStatus.BANNED, updated.accountStatus)
        assertEquals(lastLoginAt, updated.lastLoginAt)
    }

    @Test
    fun `deleteUser removes user`() = runBlocking {
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
        ExposedTestDb.tx { repository.createUser(user) }

        val deleteResult = ExposedTestDb.tx { repository.deleteUser(userId) }
        assertTrue(deleteResult is AppResult.Success)

        val getResult = ExposedTestDb.tx { repository.getUserById(userId) }
        assertTrue(getResult is AppResult.Success)
        assertNull((getResult as AppResult.Success).data)
    }

    @Test
    fun `getUsersList returns paginated users with optional filters`() = runBlocking {
        val user1 = User(
            id = UserId.generate(),
            role = UserRole.USER,
            accountStatus = UserAccountStatus.ACTIVE,
            lastLoginAt = null,
            lastActiveAt = null,
            createdAt = Instant.now(),
            updatedAt = null
        )
        val user2 = User(
            id = UserId.generate(),
            role = UserRole.ADMIN,
            accountStatus = UserAccountStatus.ACTIVE,
            lastLoginAt = null,
            lastActiveAt = null,
            createdAt = Instant.now(),
            updatedAt = null
        )
        ExposedTestDb.tx { repository.createUser(user1) }
        ExposedTestDb.tx { repository.createUser(user2) }

        val result = ExposedTestDb.tx {
            repository.getUsersList(
                params = PageParams(page = 1, size = 10),
                role = null,
                accountStatus = null
            )
        }

        assertTrue(result is AppResult.Success)
        val paged = (result as AppResult.Success).data
        assertEquals(2L, paged.totalCount)
        assertEquals(2, paged.items.size)
    }

    private companion object {
        const val CREATED_AT = "2026-01-01T12:00:00Z"
        const val LAST_LOGIN_AT = "2026-01-02T00:00:00Z"
    }
}
