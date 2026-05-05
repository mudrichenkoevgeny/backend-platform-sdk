package io.github.mudrichenkoevgeny.backend.feature.user.manager.user

import io.github.mudrichenkoevgeny.backend.core.common.model.UpdateField
import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.database.util.dbQuery
import io.github.mudrichenkoevgeny.backend.feature.user.database.repository.user.UserRepository
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.UserRoleAccessFilter
import io.github.mudrichenkoevgeny.backend.core.common.result.mapNotNullOrError
import io.github.mudrichenkoevgeny.backend.core.common.result.mapSuccess
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.provider.authsettings.AuthSettingsProvider
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.SortOrder
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.permission.PermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserSortValues
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserDetails
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.permission.UserPermissionCode
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Default [UserManager] implementation.
 *
 * Delegates persistence to [UserRepository] and wraps calls in [dbQuery].
 * When a specific user id is requested but not found, returns [UserError.UserNotFound].
 */
@Singleton
class UserManagerImpl @Inject constructor(
    private val userRepository: UserRepository,
    private val authSettingsProvider: AuthSettingsProvider
): UserManager {

    override suspend fun getUserByIdForSelf(userId: UserId): AppResult<UserDetails?> = dbQuery {
        userRepository.getUserDetailsById(userId)
    }

    override suspend fun createUser(
        role: UserRole,
        accountStatus: UserAccountStatus,
        authorityLevel: Int,
        permissions: Set<PermissionCode>
    ): AppResult<UserDetails> = dbQuery {
        val now = Clock.System.now()

        val user = UserDetails(
            id = UserId.generate(),
            role = role,
            accountStatus = accountStatus,
            accountStatusBeforeDeletion = accountStatus,
            authorityLevel = authorityLevel,
            permissionCodes = permissions,
            isTotpEnabled = false,
            lastLoginAt = now,
            lastActiveAt = now,
            createdAt = now,
            updatedAt = null,
            scheduledPermanentDeletionAt = null
        )

        userRepository.createUser(user)
    }

    override suspend fun getOrCreateUser(
        userId: UserId?,
        role: UserRole,
        accountStatus: UserAccountStatus,
        authorityLevel: Int,
        permissions: Set<PermissionCode>
    ): AppResult<UserDetails> {
        return if (userId == null) {
            createUser(
                role = role,
                accountStatus = accountStatus,
                authorityLevel = authorityLevel,
                permissions = permissions
            )
        } else {
            getUserByIdForSelf(userId).mapNotNullOrError(
                UserError.UserNotFound()
            )
        }
    }

    override suspend fun updateUserForManagement(
        user: UserDetails,
        accountStatus: UserAccountStatus?,
        authorityLevel: Int?,
        permissions: Set<PermissionCode>?
    ): AppResult<UserDetails?> = dbQuery {
        var statusBeforeDeletionUpdate: UpdateField<UserAccountStatus> = UpdateField.Ignore
        var scheduledDeletionUpdate: UpdateField<Instant> = UpdateField.Ignore

        if (accountStatus == UserAccountStatus.PENDING_DELETION
            && user.accountStatus != UserAccountStatus.PENDING_DELETION
        ) {
            statusBeforeDeletionUpdate = UpdateField.Set(user.accountStatus)
            scheduledDeletionUpdate = UpdateField.Set(getScheduledPermanentDeletionAt())
        }

        if (user.accountStatus == UserAccountStatus.PENDING_DELETION
            && accountStatus != null
            && accountStatus != UserAccountStatus.PENDING_DELETION
        ) {
            statusBeforeDeletionUpdate = UpdateField.Set(null)
            scheduledDeletionUpdate = UpdateField.Set(null)
        }

        userRepository.updateUser(
            userId = user.id,
            status = accountStatus?.let { UpdateField.Set(it) } ?: UpdateField.Ignore,
            statusBeforeDeletion = statusBeforeDeletionUpdate,
            authorityLevel = authorityLevel?.let { UpdateField.Set(it) } ?: UpdateField.Ignore,
            permissionCodes = permissions?.let { UpdateField.Set(it) } ?: UpdateField.Ignore,
            scheduledPermanentDeletionAt = scheduledDeletionUpdate
        ).mapSuccess { userDetails -> userDetails }
    }

    override suspend fun getUserForManagement(
        userId: UserId,
        managementUserId: UserId,
        managementUserPermissionCodes: Set<PermissionCode>
    ): AppResult<UserDetails?> = dbQuery {
        val getTargetUserResult = userRepository.getUserDetailsById(userId)

        when (getTargetUserResult) {
            is AppResult.Error -> getTargetUserResult
            is AppResult.Success -> {
                val targetUser = getTargetUserResult.data ?: return@dbQuery AppResult.Success(null)

                val accessFilter = buildAccessFilter(managementUserPermissionCodes)

                if (targetUser.role !in accessFilter.allowedUserRoles) {
                    return@dbQuery AppResult.Error(UserError.UserMissingPermissions(managementUserId))
                }

                AppResult.Success(targetUser)
            }
        }
    }

    override suspend fun getUsersPageForManagement(
        managementUserPermissionCodes: Set<PermissionCode>,
        pageParams: PageParams,
        sortBy: UserSortValues.UserSortBy,
        sortOrder: SortOrder,
        roles: List<UserRole>,
        accountStatuses: List<UserAccountStatus>,
        accountStatusesBeforeDeletion: List<UserAccountStatus>,
        authorityLevelFrom: Int?,
        authorityLevelTo: Int?,
        permissionCodes: Set<PermissionCode>,
        isTotpEnabled: Boolean?,
    ): AppResult<PagedResult<UserDetails>> = dbQuery {
        val accessFilter = buildAccessFilter(managementUserPermissionCodes)
        userRepository.getUsersPageWithAccessFilter(
            accessFilter = accessFilter,
            pageParams = pageParams,
            sortBy = sortBy,
            sortOrder = sortOrder,
            roles = roles,
            accountStatuses = accountStatuses,
            accountStatusesBeforeDeletion = accountStatusesBeforeDeletion,
            authorityLevelFrom = authorityLevelFrom,
            authorityLevelTo = authorityLevelTo,
            permissionCodes = permissionCodes,
            isTotpEnabled = isTotpEnabled
        )
    }

    override suspend fun restoreUserForSelf(
        userId: UserId,
        newStatus: UserAccountStatus
    ): AppResult<UserDetails> = dbQuery {
        userRepository.updateUser(
            userId = userId,
            status = UpdateField.Set(newStatus),
            statusBeforeDeletion = UpdateField.Set(null),
            scheduledPermanentDeletionAt = UpdateField.Set(null)
        )
    }

    override suspend fun scheduleUserDeletionForSelf(
        userId: UserId,
        currentStatus: UserAccountStatus
    ): AppResult<UserDetails> = dbQuery {
        userRepository.updateUser(
            userId = userId,
            status = UpdateField.Set(UserAccountStatus.PENDING_DELETION),
            statusBeforeDeletion = UpdateField.Set(currentStatus),
            scheduledPermanentDeletionAt = UpdateField.Set(getScheduledPermanentDeletionAt())
        )
    }

    override suspend fun deleteUserForManagement(userId: UserId): AppResult<Unit> = dbQuery {
        userRepository.deleteUser(userId)
    }

    override suspend fun deleteUsersDueForPermanentDeletionForSystem(): AppResult<Int> = dbQuery {
        userRepository.deleteUsersDueForPermanentDeletion(Clock.System.now())
    }

    private fun buildAccessFilter(userPermissionCodes: Set<PermissionCode>): UserRoleAccessFilter {
        val allowedUserRoles = mutableSetOf<UserRole>()

        if (userPermissionCodes.contains(UserPermissionCode.USER_GET_OF_USER)) {
            allowedUserRoles.add(UserRole.USER)
        }
        if (userPermissionCodes.contains(UserPermissionCode.USER_GET_OF_STAFF)) {
            allowedUserRoles.add(UserRole.STAFF)
        }

        return UserRoleAccessFilter(allowedUserRoles = allowedUserRoles)
    }

    private fun getScheduledPermanentDeletionAt(): Instant {
        return Clock.System.now() + authSettingsProvider.getAccountDeletionDelaySeconds().seconds
    }
}