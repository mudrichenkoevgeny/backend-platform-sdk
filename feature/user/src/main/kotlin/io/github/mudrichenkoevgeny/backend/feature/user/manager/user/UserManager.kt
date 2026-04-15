package io.github.mudrichenkoevgeny.backend.feature.user.manager.user

import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.SortOrder
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.permission.PermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserSortValues
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserDetails
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId

/**
 * Manages user entities for the user feature.
 *
 * Provides a higher-level API over persistence for creating and loading users.
 */
interface UserManager {
    /**
     * Loads a user by id.
     *
     * @param userId user id
     * @return user when found, `null` when missing, or an error
     */
    suspend fun getUserById(
        userId: UserId
    ): AppResult<UserDetails?>

    /**
     * Creates a new user.
     *
     * @param role user role
     * @param accountStatus initial account status
     * @param permissions explicit permission codes assigned to the new user
     * @return created user or an error
     */
    suspend fun createUser(
        role: UserRole,
        accountStatus: UserAccountStatus,
        permissions: Set<PermissionCode>
    ): AppResult<UserDetails>

    /**
     * Loads an existing user by [userId] or creates a new user when [userId] is `null`.
     *
     * @param userId optional user id to load
     * @param role role used when creating a new user
     * @param accountStatus status used when creating a new user
     * @param permissions permission codes used when creating a new user
     * @return loaded or created user, or an error
     */
    suspend fun getOrCreateUser(
        userId: UserId? = null,
        role: UserRole = UserRole.USER,
        accountStatus: UserAccountStatus = UserAccountStatus.ACTIVE,
        permissions: Set<PermissionCode>
    ): AppResult<UserDetails>

    /**
     * Deletes a user by id.
     *
     * @param userId user id
     * @return success or an error
     */
    suspend fun deleteUserById(
        userId: UserId
    ): AppResult<Unit>

    /**
     * Returns a paginated list of users visible for the caller and matching optional filters.
     *
     * [userPermissionCodes] are converted to a row-level access filter (allowed user roles),
     * then the repository applies [role], [accountStatus], [accountStatusBeforeDeletion], and
     * [userPermissionCode] filters on top.
     *
     * @param userPermissionCodes caller permission codes used to derive access boundaries.
     * @param pageParams one-based page and size.
     * @param sortBy user list sort field.
     * @param sortOrder sort direction.
     * @param role optional role equality filter.
     * @param accountStatus optional account status equality filter.
     * @param accountStatusBeforeDeletion optional status-before-deletion equality filter.
     * @param userPermissionCode optional filter by presence of a specific permission on user row.
     */
    suspend fun getUsersList(
        userPermissionCodes: Set<PermissionCode>,
        pageParams: PageParams,
        sortBy: UserSortValues.UserSortBy = UserSortValues.UserSortBy.CREATED_AT,
        sortOrder: SortOrder = SortOrder.DESC,
        role: UserRole? = null,
        accountStatus: UserAccountStatus? = null,
        accountStatusBeforeDeletion: UserAccountStatus? = null,
        userPermissionCode: PermissionCode? = null
    ): AppResult<PagedResult<UserDetails>>

    /**
     * Deletes users whose scheduled permanent deletion timestamp is due (<= now).
     *
     * @return number of deleted rows or an error.
     */
    suspend fun deleteUsersDueForPermanentDeletion(): AppResult<Int>
}