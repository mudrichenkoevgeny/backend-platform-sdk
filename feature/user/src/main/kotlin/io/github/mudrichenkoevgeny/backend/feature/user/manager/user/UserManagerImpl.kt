package io.github.mudrichenkoevgeny.backend.feature.user.manager.user

import io.github.mudrichenkoevgeny.backend.core.common.model.UpdateField
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.mapNotNullOrError
import io.github.mudrichenkoevgeny.backend.core.common.result.mapSuccess
import io.github.mudrichenkoevgeny.backend.core.database.util.dbQuery
import io.github.mudrichenkoevgeny.backend.core.security.lockout.LockoutManager
import io.github.mudrichenkoevgeny.backend.core.storage.manager.StorageManager
import io.github.mudrichenkoevgeny.backend.feature.user.database.repository.user.UserRepository
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.UserRoleAccessFilter
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.user.UpdateUserParams
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.network.model.UserListQueryParams
import io.github.mudrichenkoevgeny.backend.feature.user.provider.authsettings.AuthSettingsProvider
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.permission.PermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.accountlockout.AccountLockoutType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserInternal
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
    private val authSettingsProvider: AuthSettingsProvider,
    private val lockoutManager: LockoutManager,
    private val storageManager: StorageManager
) : UserManager {

    override suspend fun getUserInternalById(userId: UserId): AppResult<UserInternal?> = dbQuery {
        val userResult = userRepository.getUserInternalById(userId)
        val user = when (userResult) {
            is AppResult.Success -> userResult.data ?: return@dbQuery AppResult.Success(null)
            is AppResult.Error -> return@dbQuery userResult
        }
        AppResult.Success(enrichUserInternal(user))
    }

    override suspend fun createUser(
        role: UserRole,
        accountStatus: UserAccountStatus,
        authorityLevel: Int,
        permissions: Set<PermissionCode>,
        displayName: String?,
        nickname: String?
    ): AppResult<UserInternal> = dbQuery {
        val now = Clock.System.now()

        val user = UserInternal(
            id = UserId.generate(),
            role = role,
            accountStatus = accountStatus,
            accountStatusOnRestore = accountStatus,
            authorityLevel = authorityLevel,
            permissionCodes = permissions,
            isTotpEnabled = false,
            lockoutType = AccountLockoutType.NONE,
            temporaryLockoutUntil = null,
            lastLoginAt = now,
            lastActiveAt = now,
            createdAt = now,
            updatedAt = null,
            scheduledPermanentDeletionAt = null,
            storageConfiguration = null,
            nickname = nickname,
            displayName = displayName,
            avatarFileId = null,
            avatarUrls = null,
            birthdate = null
        )

        userRepository.createUser(user)
    }

    override suspend fun getOrCreateUser(
        userId: UserId?,
        role: UserRole,
        accountStatus: UserAccountStatus,
        authorityLevel: Int,
        permissions: Set<PermissionCode>,
        displayName: String?,
        nickname: String?
    ): AppResult<UserInternal> = if (userId == null) {
        createUser(
            role = role,
            accountStatus = accountStatus,
            authorityLevel = authorityLevel,
            permissions = permissions,
            displayName = displayName,
            nickname = nickname
        )
    } else {
        getUserInternalById(userId).mapNotNullOrError(UserError.UserNotFound(userId))
    }

    override suspend fun updateUser(
        user: UserInternal,
        updateUserParams: UpdateUserParams
    ): AppResult<UserInternal?> = dbQuery {
        val targetAccountStatus = (updateUserParams.accountStatus as? UpdateField.Set)?.value

        var statusUpdate: UpdateField<UserAccountStatus> = updateUserParams.accountStatus
        var statusOnRestoreUpdate: UpdateField<UserAccountStatus> = UpdateField.Ignore
        var scheduledDeletionUpdate: UpdateField<Instant> = UpdateField.Ignore

        if (targetAccountStatus == UserAccountStatus.PENDING_DELETION
            && user.accountStatus != UserAccountStatus.PENDING_DELETION
        ) {
            statusUpdate = UpdateField.Set(UserAccountStatus.PENDING_DELETION)
            statusOnRestoreUpdate = UpdateField.Set(user.accountStatus)
            scheduledDeletionUpdate = UpdateField.Set(getScheduledPermanentDeletionAt())
        } else if (user.accountStatus == UserAccountStatus.PENDING_DELETION
            && targetAccountStatus != null
            && targetAccountStatus != UserAccountStatus.PENDING_DELETION
        ) {
            statusUpdate = UpdateField.Ignore
            statusOnRestoreUpdate = UpdateField.Set(targetAccountStatus)
            scheduledDeletionUpdate = UpdateField.Ignore
        }

        val repositoryUpdateParams = updateUserParams.copy(
            accountStatus = statusUpdate,
            accountStatusOnRestore = statusOnRestoreUpdate,
            scheduledPermanentDeletionAt = scheduledDeletionUpdate
        )

        userRepository.updateUser(
            userId = user.id,
            updateUserParams = repositoryUpdateParams
        ).mapSuccess { userInternal -> enrichUserInternal(userInternal) }
    }

    override suspend fun getUserInternal(
        userId: UserId,
        managementUserId: UserId,
        managementUserPermissionCodes: Set<PermissionCode>
    ): AppResult<UserInternal?> = dbQuery {
        val getTargetUserResult = userRepository.getUserInternalById(userId)

        when (getTargetUserResult) {
            is AppResult.Error -> getTargetUserResult
            is AppResult.Success -> {
                val targetUser = getTargetUserResult.data ?: return@dbQuery AppResult.Success(null)

                val accessFilter = buildAccessFilter(managementUserPermissionCodes)

                if (targetUser.role !in accessFilter.allowedUserRoles) {
                    return@dbQuery AppResult.Error(UserError.UserMissingPermissions(managementUserId))
                }

                AppResult.Success(enrichUserInternal(targetUser))
            }
        }
    }

    override suspend fun getUserInternalByNickname(nickname: String): AppResult<UserInternal?> = dbQuery {
        val userResult = userRepository.getUserInternalByNickname(nickname)
        val user = when (userResult) {
            is AppResult.Success -> userResult.data ?: return@dbQuery AppResult.Success(null)
            is AppResult.Error -> return@dbQuery userResult
        }
        AppResult.Success(enrichUserInternal(user))
    }

    override suspend fun getUsersInternalPage(
        managementUserPermissionCodes: Set<PermissionCode>,
        queryParams: UserListQueryParams
    ): AppResult<PagedResult<UserInternal>> = dbQuery {
        val accessFilter = buildAccessFilter(managementUserPermissionCodes)
        val pagedResult = userRepository.getUsersPageWithAccessFilter(
            accessFilter = accessFilter,
            queryParams = queryParams
        )

        val enrichedItems = when (pagedResult) {
            is AppResult.Success -> {
                val enrichedList = pagedResult.data.items.map { item ->
                    enrichUserInternal(item)
                }
                PagedResult(
                    items = enrichedList,
                    totalCount = pagedResult.data.totalCount,
                    pageNumber = pagedResult.data.pageNumber,
                    pageSize = pagedResult.data.pageSize,
                    totalPages = pagedResult.data.totalPages
                )
            }
            is AppResult.Error -> return@dbQuery pagedResult
        }

        AppResult.Success(enrichedItems)
    }

    override suspend fun restoreUser(
        userId: UserId,
        newStatus: UserAccountStatus
    ): AppResult<UserInternal> = dbQuery {
        userRepository.updateUser(
            userId = userId,
            updateUserParams = UpdateUserParams(
                accountStatus = UpdateField.Set(newStatus),
                accountStatusOnRestore = UpdateField.Set(null),
                scheduledPermanentDeletionAt = UpdateField.Set(null)
            )
        ).mapSuccess { userInternal -> enrichUserInternal(userInternal) }
    }

    override suspend fun scheduleUserDeletion(
        userId: UserId,
        currentStatus: UserAccountStatus
    ): AppResult<UserInternal> = dbQuery {
        userRepository.updateUser(
            userId = userId,
            updateUserParams = UpdateUserParams(
                accountStatus = UpdateField.Set(UserAccountStatus.PENDING_DELETION),
                accountStatusOnRestore = UpdateField.Set(currentStatus),
                scheduledPermanentDeletionAt = UpdateField.Set(getScheduledPermanentDeletionAt())
            )
        ).mapSuccess { userInternal -> enrichUserInternal(userInternal) }
    }

    override suspend fun deleteUser(userId: UserId): AppResult<Unit> = dbQuery {
        userRepository.deleteUser(userId)
    }

    override suspend fun unlockUserAccount(
        userId: UserId,
        clearLockoutForIdentifiers: List<String>
    ): AppResult<UserInternal> = dbQuery {
        lockoutManager.clearLockout(userId.asHexDashString())

        for (identifier in clearLockoutForIdentifiers) {
            lockoutManager.clearLockout(identifier)
        }

        userRepository.updateUser(
            userId = userId,
            updateUserParams = UpdateUserParams(
                lockoutType = UpdateField.Set(AccountLockoutType.NONE),
                temporaryLockoutUntil = UpdateField.Set(null)
            )
        ).mapSuccess { userInternal -> enrichUserInternal(userInternal) }
    }

    override suspend fun lockUserAccount(
        userId: UserId,
        temporaryLockoutUntil: Instant
    ): AppResult<UserInternal> = dbQuery {
        userRepository.updateUser(
            userId = userId,
            updateUserParams = UpdateUserParams(
                lockoutType = UpdateField.Set(AccountLockoutType.TEMPORARY),
                temporaryLockoutUntil = UpdateField.Set(temporaryLockoutUntil)
            )
        ).mapSuccess { userInternal -> enrichUserInternal(userInternal) }
    }

    override suspend fun lockUserAccountIndefinitely(userId: UserId): AppResult<UserInternal> = dbQuery {
        userRepository.updateUser(
            userId = userId,
            updateUserParams = UpdateUserParams(
                lockoutType = UpdateField.Set(AccountLockoutType.INDEFINITE),
                temporaryLockoutUntil = UpdateField.Set(null)
            )
        ).mapSuccess { userInternal -> enrichUserInternal(userInternal) }
    }

    override suspend fun deleteUsersDueForPermanentDeletion(): AppResult<Int> = dbQuery {
        userRepository.deleteUsersDueForPermanentDeletion(Clock.System.now())
    }

    override suspend fun unlockExpiredAccountLockouts(): AppResult<Int> = dbQuery {
        val expiredUsersResult = userRepository.getExpiredTemporaryLockouts(Clock.System.now())
        val userIds = when (expiredUsersResult) {
            is AppResult.Success -> expiredUsersResult.data
            is AppResult.Error -> return@dbQuery expiredUsersResult
        }

        if (userIds.isEmpty()) {
            return@dbQuery AppResult.Success(0)
        }

        val identifierStrings = userIds.map { userId -> userId.asHexDashString() }
        lockoutManager.clearLockoutsBatch(identifierStrings)

        userRepository.unlockUsersBatch(userIds).mapSuccess { userIds.size }
    }

    private suspend fun enrichUserInternal(user: UserInternal): UserInternal {
        val userWithLockout = enrichUserInternalWithLockout(user)
        return enrichUserInternalWithAvatarUrls(userWithLockout)
    }

    private suspend fun enrichUserInternalWithAvatarUrls(user: UserInternal): UserInternal {
        val avatarFileId = user.avatarFileId ?: return user

        val filePrivateResult = storageManager.getSelfStorageFilePrivate(
            ownerUserId = user.id.asHexDashString(),
            fileId = avatarFileId
        )

        val avatarUrls = when (filePrivateResult) {
            is AppResult.Success -> filePrivateResult.data?.fileUrls
            is AppResult.Error -> null
        } ?: return user

        return user.copy(avatarUrls = avatarUrls)
    }

    private suspend fun enrichUserInternalWithLockout(user: UserInternal): UserInternal {
        val isIndefiniteResult = lockoutManager.isIndefiniteLockout(user.id.asHexDashString())
        val isIndefinite = when (isIndefiniteResult) {
            is AppResult.Success -> isIndefiniteResult.data
            is AppResult.Error -> false
        }

        if (isIndefinite) {
            return user.copy(
                lockoutType = AccountLockoutType.INDEFINITE,
                temporaryLockoutUntil = null
            )
        }

        val lockoutUntilResult = lockoutManager.getLockoutUntil(user.id.asHexDashString())
        val lockoutUntil = when (lockoutUntilResult) {
            is AppResult.Success -> lockoutUntilResult.data
            is AppResult.Error -> null
        }

        if (lockoutUntil != null && user.lockoutType != AccountLockoutType.INDEFINITE) {
            return user.copy(
                lockoutType = AccountLockoutType.TEMPORARY,
                temporaryLockoutUntil = lockoutUntil
            )
        }

        if (user.lockoutType == AccountLockoutType.TEMPORARY) {
            val now = Clock.System.now()
            val tempUntil = user.temporaryLockoutUntil
            if (tempUntil != null && now >= tempUntil) {
                return user.copy(
                    lockoutType = AccountLockoutType.NONE,
                    temporaryLockoutUntil = null
                )
            }
        }

        return user
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
        return Clock.System.now() + authSettingsProvider.getAccountDeletionGracePeriodSeconds().seconds
    }
}
