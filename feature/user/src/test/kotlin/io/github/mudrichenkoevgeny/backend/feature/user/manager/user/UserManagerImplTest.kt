package io.github.mudrichenkoevgeny.backend.feature.user.manager.user

import io.github.mudrichenkoevgeny.backend.core.common.model.UpdateField
import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.lockout.LockoutManager
import io.github.mudrichenkoevgeny.backend.feature.user.database.repository.user.UserRepository
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.provider.authsettings.AuthSettingsProvider
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.user.createTestUserInternal
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.SortOrder
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserSortValues
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserInternal
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.permission.UserPermissionCode
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.v1.jdbc.Database
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Clock

class UserManagerImplTest {

    private val userRepository = mockk<UserRepository>()
    private val authSettingsProvider = mockk<AuthSettingsProvider>()
    private val lockoutManager = mockk<LockoutManager>(relaxed = true)
    private val userManager = UserManagerImpl(userRepository, authSettingsProvider, lockoutManager)

    private val userId = UserId.generate()

    @BeforeEach
    fun setup() {
        Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;", driver = "org.h2.Driver")
        coEvery { lockoutManager.isIndefiniteLockout(any()) } returns AppResult.Success(false)
        coEvery { lockoutManager.getLockoutUntil(any()) } returns AppResult.Success(null)
    }

    @Test
    fun `getUserInternalById returns success`() = runTest {
        val user = createSampleUserInternal(userId)
        coEvery { userRepository.getUserInternalById(userId) } returns AppResult.Success(user)

        val result = userManager.getUserInternalById(userId)

        assertTrue(result is AppResult.Success)
        assertEquals(user, (result as AppResult.Success).data)
    }

    @Test
    fun `createUser calls repository with generated user`() = runTest {
        coEvery { userRepository.createUser(any()) } answers { AppResult.Success(firstArg()) }

        val result = userManager.createUser(
            role = UserRole.USER,
            accountStatus = UserAccountStatus.ACTIVE,
            authorityLevel = 1,
            permissions = emptySet()
        )

        assertTrue(result is AppResult.Success)
        assertEquals(UserRole.USER, (result as AppResult.Success).data.role)
    }

    @Test
    fun `getOrCreateUser returns existing user when id provided`() = runTest {
        val user = createSampleUserInternal(userId)
        coEvery { userRepository.getUserInternalById(userId) } returns AppResult.Success(user)

        val result = userManager.getOrCreateUser(
            userId = userId,
            role = UserRole.USER,
            accountStatus = UserAccountStatus.ACTIVE,
            authorityLevel = 1,
            permissions = emptySet()
        )

        assertTrue(result is AppResult.Success)
        assertEquals(userId, (result as AppResult.Success).data.id)
    }

    @Test
    fun `updateUser schedules deletion when status changes to pending`() = runTest {
        val user = createSampleUserInternal(userId, status = UserAccountStatus.ACTIVE)
        val updatedUser = user.copy(accountStatus = UserAccountStatus.PENDING_DELETION)

        coEvery { authSettingsProvider.getAccountDeletionGracePeriodSeconds() } returns 3600
        coEvery {
            userRepository.updateUser(
                userId = userId,
                status = any(),
                statusOnRestore = any(),
                authorityLevel = any(),
                permissionCodes = any(),
                scheduledPermanentDeletionAt = any()
            )
        } returns AppResult.Success(updatedUser)

        val result = userManager.updateUser(
            user = user,
            accountStatus = UserAccountStatus.PENDING_DELETION
        )

        assertTrue(result is AppResult.Success)
        assertEquals(UserAccountStatus.PENDING_DELETION, (result as AppResult.Success).data?.accountStatus)
    }

    @Test
    fun `updateUser updates accountStatusOnRestore when user is pending deletion`() = runTest {
        val user = createSampleUserInternal(
            uId = userId,
            status = UserAccountStatus.PENDING_DELETION
        ).copy(accountStatusOnRestore = UserAccountStatus.ACTIVE)

        val updatedUser = user.copy(
            accountStatus = UserAccountStatus.PENDING_DELETION,
            accountStatusOnRestore = UserAccountStatus.BANNED
        )

        coEvery {
            userRepository.updateUser(
                userId = userId,
                status = UpdateField.Ignore,
                statusOnRestore = UpdateField.Set(UserAccountStatus.BANNED),
                authorityLevel = any(),
                permissionCodes = any(),
                scheduledPermanentDeletionAt = UpdateField.Ignore,
                accountLockoutType = any(),
                temporaryLockoutUntil = any()
            )
        } returns AppResult.Success(updatedUser)

        val result = userManager.updateUser(
            user = user,
            accountStatus = UserAccountStatus.BANNED
        )

        assertTrue(result is AppResult.Success)
        val data = (result as AppResult.Success).data
        assertEquals(UserAccountStatus.PENDING_DELETION, data?.accountStatus)
        assertEquals(UserAccountStatus.BANNED, data?.accountStatusOnRestore)
    }

    @Test
    fun `getUserInternal returns error if management missing permissions`() = runTest {
        val targetUser = createSampleUserInternal(userId, role = UserRole.STAFF)
        coEvery { userRepository.getUserInternalById(userId) } returns AppResult.Success(targetUser)

        val result = userManager.getUserInternal(
            userId = userId,
            managementUserId = UserId.generate(),
            managementUserPermissionCodes = setOf(UserPermissionCode.USER_GET_OF_USER)
        )

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is UserError.UserMissingPermissions)
    }

    @Test
    fun `getUserInternal returns user if permissions match`() = runTest {
        val targetUser = createSampleUserInternal(userId, role = UserRole.USER)
        coEvery { userRepository.getUserInternalById(userId) } returns AppResult.Success(targetUser)

        val result = userManager.getUserInternal(
            userId = userId,
            managementUserId = UserId.generate(),
            managementUserPermissionCodes = setOf(UserPermissionCode.USER_GET_OF_USER)
        )

        assertTrue(result is AppResult.Success)
        assertEquals(targetUser, (result as AppResult.Success).data)
    }

    @Test
    fun `getUsersInternalPage calls repository with filter`() = runTest {
        val paged = PagedResult(emptyList<UserInternal>(), 0, 1, 10, 0)

        coEvery {
            userRepository.getUsersPageWithAccessFilter(
                accessFilter = any(),
                pageParams = any(),
                sortBy = any(),
                sortOrder = any(),
                roles = any(),
                accountStatuses = any(),
                accountStatusesOnRestore = any(),
                authorityLevelFrom = any(),
                authorityLevelTo = any(),
                permissionCodes = any(),
                isTotpEnabled = any(),
                accountLockoutTypes = any()
            )
        } returns AppResult.Success(paged)

        val result = userManager.getUsersInternalPage(
            managementUserPermissionCodes = setOf(UserPermissionCode.USER_GET_OF_USER),
            pageParams = PageParams(1, 10),
            sortBy = UserSortValues.UserSortBy.CREATED_AT,
            sortOrder = SortOrder.DESC,
            roles = emptyList(),
            accountStatuses = emptyList(),
            accountStatusesOnRestore = emptyList(),
            authorityLevelFrom = null,
            authorityLevelTo = null,
            permissionCodes = emptySet(),
            isTotpEnabled = null
        )

        assertTrue(result is AppResult.Success)
    }

    @Test
    fun `restoreUser updates status`() = runTest {
        val user = createSampleUserInternal(userId)
        coEvery {
            userRepository.updateUser(
                userId = userId,
                status = any(),
                statusOnRestore = any(),
                scheduledPermanentDeletionAt = any()
            )
        } returns AppResult.Success(user)

        val result = userManager.restoreUser(userId, UserAccountStatus.ACTIVE)

        assertTrue(result is AppResult.Success)
    }

    @Test
    fun `scheduleUserDeletion calls update`() = runTest {
        val user = createSampleUserInternal(userId)
        coEvery { authSettingsProvider.getAccountDeletionGracePeriodSeconds() } returns 3600
        coEvery {
            userRepository.updateUser(
                userId = userId,
                status = any(),
                statusOnRestore = any(),
                scheduledPermanentDeletionAt = any()
            )
        } returns AppResult.Success(user)

        val result = userManager.scheduleUserDeletion(userId, UserAccountStatus.ACTIVE)

        assertTrue(result is AppResult.Success)
    }

    @Test
    fun `deleteUser calls repository`() = runTest {
        coEvery { userRepository.deleteUser(userId) } returns AppResult.Success(Unit)

        val result = userManager.deleteUser(userId)

        assertTrue(result is AppResult.Success)
    }

    @Test
    fun `deleteUsersDueForPermanentDeletion returns deleted count`() = runTest {
        coEvery { userRepository.deleteUsersDueForPermanentDeletion(any()) } returns AppResult.Success(5)

        val result = userManager.deleteUsersDueForPermanentDeletion()

        assertTrue(result is AppResult.Success)
        assertEquals(5, (result as AppResult.Success).data)
    }

    private fun createSampleUserInternal(
        uId: UserId,
        role: UserRole = UserRole.USER,
        status: UserAccountStatus = UserAccountStatus.ACTIVE
    ) = createTestUserInternal(
        id = uId,
        role = role,
        accountStatus = status,
        accountStatusOnRestore = status,
        authorityLevel = 1,
        lastLoginAt = Clock.System.now(),
        lastActiveAt = Clock.System.now()
    )
}
