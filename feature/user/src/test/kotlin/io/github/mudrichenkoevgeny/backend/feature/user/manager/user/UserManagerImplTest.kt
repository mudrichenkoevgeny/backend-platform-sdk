package io.github.mudrichenkoevgeny.backend.feature.user.manager.user

import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.database.repository.user.UserRepository
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.provider.authsettings.AuthSettingsProvider
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.SortOrder
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserSortValues
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserDetails
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.permission.UserPermissionCode
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.time.Clock

class UserManagerImplTest {

    private val userRepository = mockk<UserRepository>()
    private val authSettingsProvider = mockk<AuthSettingsProvider>()
    private val userManager = UserManagerImpl(userRepository, authSettingsProvider)

    private val userId = UserId.generate()

    @Test
    fun `getUserByIdForSelf returns success`() = runTest {
        val user = createSampleUserDetails(userId)
        coEvery { userRepository.getUserDetailsById(userId) } returns AppResult.Success(user)

        val result = userManager.getUserByIdForSelf(userId)

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
        val user = createSampleUserDetails(userId)
        coEvery { userRepository.getUserDetailsById(userId) } returns AppResult.Success(user)

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
    fun `updateUserForManagement schedules deletion when status changes to pending`() = runTest {
        val user = createSampleUserDetails(userId, status = UserAccountStatus.ACTIVE)
        val updatedUser = user.copy(accountStatus = UserAccountStatus.PENDING_DELETION)

        coEvery { authSettingsProvider.getAccountDeletionDelaySeconds() } returns 3600
        coEvery {
            userRepository.updateUser(
                userId = userId,
                status = any(),
                statusBeforeDeletion = any(),
                authorityLevel = any(),
                permissionCodes = any(),
                scheduledPermanentDeletionAt = any()
            )
        } returns AppResult.Success(updatedUser)

        val result = userManager.updateUserForManagement(
            user = user,
            accountStatus = UserAccountStatus.PENDING_DELETION
        )

        assertTrue(result is AppResult.Success)
        assertEquals(UserAccountStatus.PENDING_DELETION, (result as AppResult.Success).data?.accountStatus)
    }

    @Test
    fun `getUserForManagement returns error if management missing permissions`() = runTest {
        val targetUser = createSampleUserDetails(userId, role = UserRole.STAFF)
        coEvery { userRepository.getUserDetailsById(userId) } returns AppResult.Success(targetUser)

        val result = userManager.getUserForManagement(
            userId = userId,
            managementUserId = UserId.generate(),
            managementUserPermissionCodes = setOf(UserPermissionCode.USER_GET_OF_USER)
        )

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is UserError.UserMissingPermissions)
    }

    @Test
    fun `getUserForManagement returns user if permissions match`() = runTest {
        val targetUser = createSampleUserDetails(userId, role = UserRole.USER)
        coEvery { userRepository.getUserDetailsById(userId) } returns AppResult.Success(targetUser)

        val result = userManager.getUserForManagement(
            userId = userId,
            managementUserId = UserId.generate(),
            managementUserPermissionCodes = setOf(UserPermissionCode.USER_GET_OF_USER)
        )

        assertTrue(result is AppResult.Success)
        assertEquals(targetUser, (result as AppResult.Success).data)
    }

    @Test
    fun `getUsersPageForManagement calls repository with filter`() = runTest {
        val paged = PagedResult(emptyList<UserDetails>(), 0, 1, 10, 0)

        coEvery {
            userRepository.getUsersPageWithAccessFilter(
                accessFilter = any(),
                pageParams = any(),
                sortBy = any(),
                sortOrder = any(),
                roles = any(),
                accountStatuses = any(),
                accountStatusesBeforeDeletion = any(),
                authorityLevelFrom = any(),
                authorityLevelTo = any(),
                permissionCodes = any(),
                isTotpEnabled = any()
            )
        } returns AppResult.Success(paged)

        val result = userManager.getUsersPageForManagement(
            managementUserPermissionCodes = setOf(UserPermissionCode.USER_GET_OF_USER),
            pageParams = PageParams(1, 10),
            sortBy = UserSortValues.UserSortBy.CREATED_AT,
            sortOrder = SortOrder.DESC,
            roles = emptyList(),
            accountStatuses = emptyList(),
            accountStatusesBeforeDeletion = emptyList(),
            authorityLevelFrom = null,
            authorityLevelTo = null,
            permissionCodes = emptySet(),
            isTotpEnabled = null
        )

        assertTrue(result is AppResult.Success)
    }

    @Test
    fun `restoreUserForSelf updates status`() = runTest {
        val user = createSampleUserDetails(userId)
        coEvery {
            userRepository.updateUser(
                userId = userId,
                status = any(),
                statusBeforeDeletion = any(),
                scheduledPermanentDeletionAt = any()
            )
        } returns AppResult.Success(user)

        val result = userManager.restoreUserForSelf(userId, UserAccountStatus.ACTIVE)

        assertTrue(result is AppResult.Success)
    }

    @Test
    fun `scheduleUserDeletionForSelf calls update`() = runTest {
        val user = createSampleUserDetails(userId)
        coEvery { authSettingsProvider.getAccountDeletionDelaySeconds() } returns 3600
        coEvery {
            userRepository.updateUser(
                userId = userId,
                status = any(),
                statusBeforeDeletion = any(),
                scheduledPermanentDeletionAt = any()
            )
        } returns AppResult.Success(user)

        val result = userManager.scheduleUserDeletionForSelf(userId, UserAccountStatus.ACTIVE)

        assertTrue(result is AppResult.Success)
    }

    @Test
    fun `deleteUserForManagement calls repository`() = runTest {
        coEvery { userRepository.deleteUser(userId) } returns AppResult.Success(Unit)

        val result = userManager.deleteUserForManagement(userId)

        assertTrue(result is AppResult.Success)
    }

    @Test
    fun `deleteUsersDueForPermanentDeletionForSystem returns deleted count`() = runTest {
        coEvery { userRepository.deleteUsersDueForPermanentDeletion(any()) } returns AppResult.Success(5)

        val result = userManager.deleteUsersDueForPermanentDeletionForSystem()

        assertTrue(result is AppResult.Success)
        assertEquals(5, (result as AppResult.Success).data)
    }

    private fun createSampleUserDetails(
        uId: UserId,
        role: UserRole = UserRole.USER,
        status: UserAccountStatus = UserAccountStatus.ACTIVE
    ) = UserDetails(
        id = uId,
        role = role,
        accountStatus = status,
        accountStatusBeforeDeletion = status,
        authorityLevel = 1,
        permissionCodes = emptySet(),
        isTotpEnabled = false,
        lastLoginAt = Clock.System.now(),
        lastActiveAt = Clock.System.now(),
        createdAt = Clock.System.now(),
        updatedAt = null,
        scheduledPermanentDeletionAt = null
    )
}