package io.github.mudrichenkoevgeny.backend.feature.user.manager.user

import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.SortOrder
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.permission.PermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.accountlockout.AccountLockoutType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserSortValues
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserInternal
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.model.user.UserStorageConfigurationPayload
import kotlin.time.Instant

/**
 * Manages user entities for the user feature.
 *
 * Provides a higher-level API over persistence for creating, updating, and loading users.
 * Handles role-based visibility and administrative boundaries for management operations.
 */
interface UserManager {
    /**
     * Loads a user by their ID as an internal domain model.
     *
     * @param userId The unique identifier of the user.
     * @return [AppResult.Success] with [UserInternal] when found, null when missing, or an error.
     */
    suspend fun getUserInternalById(
        userId: UserId
    ): AppResult<UserInternal?>

    /**
     * Creates a new user entity.
     *
     * @param role User role.
     * @param accountStatus Initial account status.
     * @param authorityLevel The numerical rank of the user's authority.
     * @param permissions Explicit permission codes assigned to the new user.
     * @return [AppResult.Success] with the created [UserInternal] or an error.
     */
    suspend fun createUser(
        role: UserRole,
        accountStatus: UserAccountStatus,
        authorityLevel: Int,
        permissions: Set<PermissionCode>
    ): AppResult<UserInternal>

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
     * @return [AppResult.Success] with the loaded or created [UserInternal], or an error.
     */
    suspend fun getOrCreateUser(
        userId: UserId? = null,
        role: UserRole = UserRole.USER,
        accountStatus: UserAccountStatus = UserAccountStatus.ACTIVE,
        authorityLevel: Int,
        permissions: Set<PermissionCode>
    ): AppResult<UserInternal>

    /**
     * Updates user details from an administrative context.
     *
     * Allows modification of sensitive fields like status, authority level, permissions, and storage configuration.
     *
     * @param user The current internal user instance to update.
     * @param accountStatus New account status if update is requested.
     * @param authorityLevel New authority level if update is requested.
     * @param permissions New set of permission codes if update is requested.
     * @param lockoutType New account lockout type if update is requested.
     * @param temporaryLockoutUntil New temporary lockout expiration timestamp in epoch milliseconds if update is requested.
     * @param storageConfiguration New storage configuration overrides if update is requested.
     * @return [AppResult.Success] with the updated [UserInternal], null if user not found, or an error.
     */
    suspend fun updateUser(
        user: UserInternal,
        accountStatus: UserAccountStatus? = null,
        authorityLevel: Int? = null,
        permissions: Set<PermissionCode>? = null,
        lockoutType: AccountLockoutType? = null,
        temporaryLockoutUntil: Long? = null,
        storageConfiguration: UserStorageConfigurationPayload? = null
    ): AppResult<UserInternal?>

    /**
     * Returns a single user's internal model for management purposes.
     *
     * Applies role-scoped visibility checks: if the target user's role is not within
     * the caller's allowed administrative scope, an error is returned.
     *
     * @param userId The ID of the user to retrieve.
     * @param managementUserId The ID of the manager/admin performing the request.
     * @param managementUserPermissionCodes Permissions used to derive access boundaries.
     * @return [AppResult.Success] with [UserInternal] if accessible, null if not found, or an error.
     */
    suspend fun getUserInternal(
        userId: UserId,
        managementUserId: UserId,
        managementUserPermissionCodes: Set<PermissionCode>
    ): AppResult<UserInternal?>

    /**
     * Returns a paginated list of internal user models visible to the caller based on management scope.
     *
     * @param managementUserPermissionCodes Caller permission codes used to derive access boundaries.
     * @param pageParams Pagination settings (page index and size).
     * @param sortBy Field to sort the user list by.
     * @param sortOrder Sorting direction.
     * @param roles Optional filters for specific user roles.
     * @param accountStatuses Optional filters for current account statuses.
     * @param accountStatusesOnRestore Optional filters for target status assigned on account restore.
     * @param authorityLevelFrom inclusive lower bound for authority level.
     * @param authorityLevelTo inclusive upper bound for authority level.
     * @param permissionCodes Optional filter for users possessing ALL specified permissions.
     * @param isTotpEnabled filter by TOTP enablement status.
     * @param accountLockoutTypes Filter by account lockout types.
     * @return [AppResult.Success] with a [PagedResult] of [UserInternal] or an error.
     */
    suspend fun getUsersInternalPage(
        managementUserPermissionCodes: Set<PermissionCode>,
        pageParams: PageParams,
        sortBy: UserSortValues.UserSortBy = UserSortValues.UserSortBy.DEFAULT,
        sortOrder: SortOrder = SortOrder.DEFAULT,
        roles: List<UserRole> = emptyList(),
        accountStatuses: List<UserAccountStatus> = emptyList(),
        accountStatusesOnRestore: List<UserAccountStatus> = emptyList(),
        authorityLevelFrom: Int? = null,
        authorityLevelTo: Int? = null,
        permissionCodes: Set<PermissionCode> = emptySet(),
        isTotpEnabled: Boolean? = null,
        accountLockoutTypes: List<AccountLockoutType> = emptyList()
    ): AppResult<PagedResult<UserInternal>>

    /**
     * Restores a user who was previously scheduled for deletion.
     *
     * @param userId The ID of the user to restore.
     * @param newStatus The account status to set after restoration.
     * @return [AppResult.Success] with the restored [UserInternal] or an error.
     */
    suspend fun restoreUser(
        userId: UserId,
        newStatus: UserAccountStatus
    ): AppResult<UserInternal>

    /**
     * Schedules a user account for permanent deletion at a future date.
     *
     * Typically, moves the user into a DELETED or PENDING_DELETION status.
     *
     * @param userId The ID of the user requesting deletion.
     * @param currentStatus The current status of the user before scheduling.
     * @return [AppResult.Success] with the updated [UserInternal] or an error.
     */
    suspend fun scheduleUserDeletion(
        userId: UserId,
        currentStatus: UserAccountStatus
    ): AppResult<UserInternal>

    /**
     * Permanently deletes a user record from the system immediately.
     *
     * This is a management operation and ignores scheduled delay.
     *
     * @param userId The ID of the user to delete.
     * @return [AppResult.Success] containing [Unit] or an error.
     */
    suspend fun deleteUser(
        userId: UserId
    ): AppResult<Unit>

    /**
     * Unlocks a temporarily locked user account.
     *
     * @param userId The ID of the user to unlock.
     * @param clearLockoutForIdentifiers Optional list of identifiers (e.g., email, phone) to clear from lockout cache.
     * @return [AppResult.Success] with the updated [UserInternal] or an error.
     */
    suspend fun unlockUserAccount(
        userId: UserId,
        clearLockoutForIdentifiers: List<String> = emptyList()
    ): AppResult<UserInternal>

    /**
     * Locks a user account temporarily until [temporaryLockoutUntil].
     *
     * @param userId The ID of the user to lock.
     * @param temporaryLockoutUntil The timestamp when lockout expires.
     * @return [AppResult.Success] with the updated [UserInternal] or an error.
     */
    suspend fun lockUserAccount(
        userId: UserId,
        temporaryLockoutUntil: Instant
    ): AppResult<UserInternal>

    /**
     * Locks a user account indefinitely under an indefinite lockout.
     *
     * @param userId The ID of the user to lock indefinitely.
     * @return [AppResult.Success] with the updated [UserInternal] or an error.
     */
    suspend fun lockUserAccountIndefinitely(
        userId: UserId
    ): AppResult<UserInternal>

    /**
     * Identifies and removes users whose scheduled permanent deletion timestamp has passed.
     *
     * Intended for use by background system tasks.
     *
     * @return [AppResult.Success] with the count of deleted rows or an error.
     */
    suspend fun deleteUsersDueForPermanentDeletion(): AppResult<Int>

    /**
     * Identifies and unlocks user accounts whose temporary lockout timestamp has passed.
     *
     * Intended for use by background system tasks.
     *
     * @return [AppResult.Success] with the count of unlocked accounts or an error.
     */
    suspend fun unlockExpiredAccountLockouts(): AppResult<Int>
}
