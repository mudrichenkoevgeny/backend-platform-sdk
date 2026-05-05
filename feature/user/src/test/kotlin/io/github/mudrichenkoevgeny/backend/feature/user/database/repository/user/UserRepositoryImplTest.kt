package io.github.mudrichenkoevgeny.backend.feature.user.database.repository.user

import io.github.mudrichenkoevgeny.backend.core.common.model.UpdateField
import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.util.createTestDataSource
import io.github.mudrichenkoevgeny.backend.feature.user.database.table.UsersTable
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.UserRoleAccessFilter
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.SortOrder
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.permission.PermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserSortValues
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserDetails
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserRepositoryImplTest {

    private val dataSource = createTestDataSource("user_repo")
    private lateinit var repository: UserRepository

    @BeforeAll
    fun setup() {
        Database.connect(dataSource)
        runBlocking {
            suspendTransaction {
                SchemaUtils.drop(UsersTable)
                SchemaUtils.create(UsersTable)
            }
        }
        repository = UserRepositoryImpl()
    }

    @Test
    fun `createUser persists and getUserDetailsById returns same user`() = runBlocking {
        val user = createTestUser(UserId.generate())

        suspendTransaction { repository.createUser(user) }
        val result = suspendTransaction { repository.getUserDetailsById(user.id) }

        val success = result as AppResult.Success
        assertNotNull(success.data)
        assertEquals(user.id, success.data!!.id)
    }

    @Test
    fun `updateUser updates all fields and returns updated user`() = runBlocking {
        val userId = UserId.generate()
        val user = createTestUser(userId)
        suspendTransaction { repository.createUser(user) }

        val newLoginAt = Instant.parse(LAST_LOGIN_AT)
        val newActiveAt = newLoginAt + 1.days
        val deletionAt = newLoginAt + 30.days
        val permissions = setOf(PermissionCode("TEST_CODE"))

        val result = suspendTransaction {
            repository.updateUser(
                userId = userId,
                status = UpdateField.Set(UserAccountStatus.BANNED),
                statusBeforeDeletion = UpdateField.Set(UserAccountStatus.ACTIVE),
                authorityLevel = UpdateField.Set(10),
                permissionCodes = UpdateField.Set(permissions),
                isTotpEnabled = UpdateField.Set(true),
                lastLoginAt = UpdateField.Set(newLoginAt),
                lastActiveAt = UpdateField.Set(newActiveAt),
                scheduledPermanentDeletionAt = UpdateField.Set(deletionAt)
            )
        }

        val updated = (result as AppResult.Success).data
        assertEquals(UserAccountStatus.BANNED, updated.accountStatus)
        assertEquals(UserAccountStatus.ACTIVE, updated.accountStatusBeforeDeletion)
        assertEquals(10, updated.authorityLevel)
        assertEquals(permissions, updated.permissionCodes)
        assertTrue(updated.isTotpEnabled)
        assertEquals(newLoginAt, updated.lastLoginAt)
        assertEquals(newActiveAt, updated.lastActiveAt)
        assertEquals(deletionAt, updated.scheduledPermanentDeletionAt)
        assertNotNull(updated.updatedAt)
    }

    @Test
    fun `getUsersPageWithAccessFilter applies all provided filters`() = runBlocking {
        val userId = UserId.generate()
        val permission = PermissionCode("FILTER_ME")
        val user = createTestUser(userId).copy(
            role = UserRole.ADMIN,
            accountStatus = UserAccountStatus.ACTIVE,
            authorityLevel = 50,
            permissionCodes = setOf(permission),
            isTotpEnabled = true
        )

        suspendTransaction { repository.createUser(user) }

        val result = suspendTransaction {
            repository.getUsersPageWithAccessFilter(
                accessFilter = UserRoleAccessFilter(allowedUserRoles = setOf(UserRole.ADMIN)),
                pageParams = PageParams(page = 1, size = 10),
                sortBy = UserSortValues.UserSortBy.CREATED_AT,
                sortOrder = SortOrder.DESC,
                roles = listOf(UserRole.ADMIN),
                accountStatuses = listOf(UserAccountStatus.ACTIVE),
                accountStatusesBeforeDeletion = emptyList(),
                authorityLevelFrom = 40,
                authorityLevelTo = 60,
                permissionCodes = setOf(permission),
                isTotpEnabled = true
            )
        }

        val success = result as AppResult.Success
        assertEquals(1, success.data.items.size)
        assertEquals(userId, success.data.items.first().id)
    }

    @Test
    fun `deleteUsersDueForPermanentDeletion deletes only users before asOf`() = runBlocking {
        val baseTime = Instant.parse(CREATED_AT)
        val expired = createTestUser(UserId.generate()).copy(scheduledPermanentDeletionAt = baseTime - 1.days)
        val notExpired = createTestUser(UserId.generate()).copy(scheduledPermanentDeletionAt = baseTime + 1.days)

        suspendTransaction {
            repository.createUser(expired)
            repository.createUser(notExpired)
        }

        val deletedResult = suspendTransaction {
            repository.deleteUsersDueForPermanentDeletion(asOf = baseTime)
        }

        assertEquals(1, (deletedResult as AppResult.Success).data)

        val checkExpired = suspendTransaction { repository.getUserDetailsById(expired.id) }
        val checkActive = suspendTransaction { repository.getUserDetailsById(notExpired.id) }

        assertNull((checkExpired as AppResult.Success).data)
        assertNotNull((checkActive as AppResult.Success).data)
    }

    @Test
    fun `deleteUser removes user record`() = runBlocking {
        val userId = UserId.generate()
        suspendTransaction { repository.createUser(createTestUser(userId)) }

        suspendTransaction { repository.deleteUser(userId) }
        val result = suspendTransaction { repository.getUserDetailsById(userId) }

        assertNull((result as AppResult.Success).data)
    }

    private fun createTestUser(userId: UserId) = UserDetails(
        id = userId,
        role = UserRole.USER,
        accountStatus = UserAccountStatus.ACTIVE,
        accountStatusBeforeDeletion = null,
        authorityLevel = 1,
        permissionCodes = emptySet(),
        isTotpEnabled = false,
        lastLoginAt = null,
        lastActiveAt = null,
        createdAt = Instant.parse(CREATED_AT),
        updatedAt = null,
        scheduledPermanentDeletionAt = null
    )

    private companion object {
        const val CREATED_AT = "2026-01-01T00:00:00Z"
        const val LAST_LOGIN_AT = "2026-01-02T00:00:00Z"
    }
}