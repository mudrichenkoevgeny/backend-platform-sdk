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
 * Provides a higher-level API over persistence for creating, updating, and loading users.
 * Handles role-based visibility and administrative boundaries for management operations.
 */
interface UserManager {
    /**
     * Loads a user by their ID for self-service scenarios.
     *
     * @param userId The unique identifier of the user.
     * @return [AppResult.Success] with [UserDetails] when found, null when missing, or an error.
     */
    suspend fun getUserByIdForSelf(
        userId: UserId
    ): AppResult<UserDetails?>

    /**
     * Creates a new user entity.
     *
     * @param role User role.
     * @param accountStatus Initial account status.
     * @param authorityLevel The numerical rank of the user's authority.
     * @param permissions Explicit permission codes assigned to the new user.
     * @return [AppResult.Success] with the created [UserDetails] or an error.
     */
    suspend fun createUser(
        role: UserRole,
        accountStatus: UserAccountStatus,
        authorityLevel: Int,
        permissions: Set<PermissionCode>
    ): AppResult<UserDetails>

    /**
     * Loads an existing user by [userId] or creates a new one if [userId] is null.
     *
     * Useful for idempotent authentication flows where user existence must be ensured.
     *
     * @param userId Optional user ID to load.
     * @param role Role used if a new user is created. Defaults to [UserRole.USER].
     * @param accountStatus Status used if a new user is created. Defaults to [UserAccountStatus.ACTIVE].
     * @param authorityLevel The numerical rank of the user's authority.
     * @param permissions Permission codes assigned if a new user is created.
     * @return [AppResult.Success] with the loaded or created [UserDetails], or an error.
     */
    suspend fun getOrCreateUser(
        userId: UserId? = null,
        role: UserRole = UserRole.USER,
        accountStatus: UserAccountStatus = UserAccountStatus.ACTIVE,
        authorityLevel: Int,
        permissions: Set<PermissionCode>
    ): AppResult<UserDetails>

    /**
     * Updates user details from an administrative context.
     *
     * Allows modification of sensitive fields like status, authority level, and permissions.
     *
     * @param user The current user details to update.
     * @param accountStatus New account status if update is requested.
     * @param authorityLevel New authority level if update is requested.
     * @param permissions New set of permission codes if update is requested.
     * @return [AppResult.Success] with the updated [UserDetails], null if user not found, or an error.
     */
    suspend fun updateUserForManagement(
        user: UserDetails,
        accountStatus: UserAccountStatus? = null,
        authorityLevel: Int? = null,
        permissions: Set<PermissionCode>? = null
    ): AppResult<UserDetails?>

    /**
     * Returns a single user's details for management purposes.
     *
     * Applies role-scoped visibility checks: if the target user's role is not within
     * the caller's allowed administrative scope, an error is returned.
     *
     * @param userId The ID of the user to retrieve.
     * @param managementUserId The ID of the manager/admin performing the request.
     * @param managementUserPermissionCodes Permissions used to derive access boundaries.
     * @return [AppResult.Success] with [UserDetails] if accessible, null if not found, or an error.
     */
    suspend fun getUserForManagement(
        userId: UserId,
        managementUserId: UserId,
        managementUserPermissionCodes: Set<PermissionCode>
    ): AppResult<UserDetails?>

    /**
     * Returns a paginated list of users visible to the caller based on management scope.
     *
     * @param managementUserPermissionCodes Caller permission codes used to derive access boundaries.
     * @param pageParams Pagination settings (page index and size).
     * @param sortBy Field to sort the user list by.
     * @param sortOrder Sorting direction.
     * @param roles Optional filters for specific user roles.
     * @param accountStatuses Optional filters for current account statuses.
     * @param accountStatusesBeforeDeletion Optional filters for status-before-deletion.
     * @param authorityLevelFrom inclusive lower bound for authority level.
     * @param authorityLevelTo inclusive upper bound for authority level.
     * @param permissionCodes Optional filter for users possessing ALL specified permissions.
     * @param isTotpEnabled filter by TOTP enablement status.
     * @return [AppResult.Success] with a [PagedResult] of users or an error.
     */
    suspend fun getUsersPageForManagement(
        managementUserPermissionCodes: Set<PermissionCode>,
        pageParams: PageParams,
        sortBy: UserSortValues.UserSortBy = UserSortValues.UserSortBy.CREATED_AT,
        sortOrder: SortOrder = SortOrder.DESC,
        roles: List<UserRole> = emptyList(),
        accountStatuses: List<UserAccountStatus> = emptyList(),
        accountStatusesBeforeDeletion: List<UserAccountStatus> = emptyList(),
        authorityLevelFrom: Int? = null,
        authorityLevelTo: Int? = null,
        permissionCodes: Set<PermissionCode> = emptySet(),
        isTotpEnabled: Boolean? = null
    ): AppResult<PagedResult<UserDetails>>

    /**
     * Restores a user who was previously scheduled for deletion.
     *
     * @param userId The ID of the user to restore.
     * @param newStatus The account status to set after restoration.
     * @return [AppResult.Success] with the restored [UserDetails] or an error.
     */
    suspend fun restoreUserForSelf(
        userId: UserId,
        newStatus: UserAccountStatus
    ): AppResult<UserDetails>

    /**
     * Schedules a user account for permanent deletion at a future date.
     *
     * Typically, moves the user into a DELETED or PENDING_DELETION status.
     *
     * @param userId The ID of the user requesting deletion.
     * @param currentStatus The current status of the user before scheduling.
     * @return [AppResult.Success] with the updated [UserDetails] or an error.
     */
    suspend fun scheduleUserDeletionForSelf(
        userId: UserId,
        currentStatus: UserAccountStatus
    ): AppResult<UserDetails>

    /**
     * Permanently deletes a user record from the system immediately.
     *
     * This is a management operation and ignores scheduled delay.
     *
     * @param userId The ID of the user to delete.
     * @return [AppResult.Success] containing [Unit] or an error.
     */
    suspend fun deleteUserForManagement(
        userId: UserId
    ): AppResult<Unit>

    /**
     * Identifies and removes users whose scheduled permanent deletion timestamp has passed.
     *
     * Intended for use by background system tasks.
     *
     * @return [AppResult.Success] with the count of deleted rows or an error.
     */
    suspend fun deleteUsersDueForPermanentDeletionForSystem(): AppResult<Int>
}