package io.github.mudrichenkoevgeny.backend.feature.user.manager.user

import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.database.util.dbQuery
import io.github.mudrichenkoevgeny.backend.feature.user.database.repository.user.UserRepository
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.UserRoleAccessFilter
import io.github.mudrichenkoevgeny.backend.core.common.result.mapNotNullOrError
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
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

/**
 * Default [UserManager] implementation.
 *
 * Delegates persistence to [UserRepository] and wraps calls in [dbQuery].
 * When a specific user id is requested but not found, returns [UserError.UserNotFound].
 */
@Singleton
class UserManagerImpl @Inject constructor(
    private val userRepository: UserRepository
): UserManager {

    override suspend fun getUserById(userId: UserId): AppResult<UserDetails?> = dbQuery {
        userRepository.getUserById(userId)
    }

    override suspend fun createUser(
        role: UserRole,
        accountStatus: UserAccountStatus,
        permissions: Set<PermissionCode>
    ): AppResult<UserDetails> = dbQuery {
        val now = Clock.System.now()

        val user = UserDetails(
            id = UserId.generate(),
            role = role,
            accountStatus = accountStatus,
            accountStatusBeforeDeletion = accountStatus,
            permissions = permissions,
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
        permissions: Set<PermissionCode>
    ): AppResult<UserDetails> {
        return if (userId == null) {
            createUser(
                role = role,
                accountStatus = accountStatus,
                permissions = permissions
            )
        } else {
            getUserById(userId).mapNotNullOrError(
                UserError.UserNotFound()
            )
        }
    }

    override suspend fun deleteUserById(userId: UserId): AppResult<Unit> = dbQuery {
        userRepository.deleteUser(userId)
    }

    override suspend fun getUsersList(
        userPermissionCodes: Set<PermissionCode>,
        pageParams: PageParams,
        sortBy: UserSortValues.UserSortBy,
        sortOrder: SortOrder,
        role: UserRole?,
        accountStatus: UserAccountStatus?,
        accountStatusBeforeDeletion: UserAccountStatus?,
        userPermissionCode: PermissionCode?
    ): AppResult<PagedResult<UserDetails>> = dbQuery {
        val accessFilter = buildAccessFilter(userPermissionCodes)
        userRepository.getUsersList(
            accessFilter = accessFilter,
            pageParams = pageParams,
            sortBy = sortBy,
            sortOrder = sortOrder,
            role = role,
            accountStatus = accountStatus,
            accountStatusBeforeDeletion = accountStatusBeforeDeletion,
            userPermissionCode = userPermissionCode
        )
    }

    override suspend fun deleteUsersDueForPermanentDeletion(): AppResult<Int> = dbQuery {
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
}