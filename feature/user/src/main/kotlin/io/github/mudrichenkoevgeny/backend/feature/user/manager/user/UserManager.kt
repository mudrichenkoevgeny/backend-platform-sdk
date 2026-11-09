package io.github.mudrichenkoevgeny.backend.feature.user.manager.user

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.user.CreateUserAccountParams
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.user.UpdateUserParams
import io.github.mudrichenkoevgeny.backend.feature.user.network.model.UserListQueryParams
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.permission.PermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserInternal
import kotlin.time.Instant

/**
 * Manages user entities for the user feature.
 *
 * Provides a higher-level API over persistence for creating, updating, and loading users.
 * Handles role-based visibility and administrative boundaries for management operations.
 */
public interface UserManager {
    /**
     * Loads a user by their ID as an internal domain model.
     *
     * @param userId The unique identifier of the user.
     * @return [AppResult.Success] with [UserInternal] when found, null when missing, or an error.
     */
    public suspend fun getUserInternalById(userId: UserId): AppResult<UserInternal?>

    /**
     * Creates a new user entity profile using [createUserAccountParams].
     *
     * @param createUserAccountParams Parameter holder containing initial user profile settings.
     * @return [AppResult.Success] with the created [UserInternal] or an error.
     */
    public suspend fun createUser(
        createUserAccountParams: CreateUserAccountParams
    ): AppResult<UserInternal>

    /**
     * Loads an existing user by [userId] or creates a new one if [userId] is null.
     *
     * Useful for idempotent authentication flows where user existence must be ensured.
     *
     * @param userId Optional user ID to load.
     * @param createUserAccountParams Parameter holder containing initial user profile settings for new user creation.
     * @return [AppResult.Success] with the loaded or created [UserInternal], or an error.
     */
    public suspend fun getOrCreateUser(
        userId: UserId? = null,
        createUserAccountParams: CreateUserAccountParams
    ): AppResult<UserInternal>

    /**
     * Updates user details from an administrative context using [updateUserParams].
     *
     * Allows modification of sensitive fields like status, authority level, permissions, and storage configuration.
     *
     * @param user The current internal user instance to update.
     * @param updateUserParams The structured update parameters holder.
     * @return [AppResult.Success] with the updated [UserInternal], null if user not found, or an error.
     */
    public suspend fun updateUser(
        user: UserInternal,
        updateUserParams: UpdateUserParams
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
    public suspend fun getUserInternal(
        userId: UserId,
        managementUserId: UserId,
        managementUserPermissionCodes: Set<PermissionCode>
    ): AppResult<UserInternal?>

    /**
     * Retrieves user internal model by nickname.
     *
     * @param nickname user nickname string.
     * @return [AppResult.Success] with [UserInternal] if found, null if missing, or an error.
     */
    public suspend fun getUserInternalByNickname(nickname: String): AppResult<UserInternal?>

    /**
     * Returns a paginated list of internal user models visible to the caller based on management scope.
     *
     * @param managementUserPermissionCodes Caller permission codes used to derive access boundaries.
     * @param queryParams Search, filter, pagination, and sorting parameters.
     * @return [AppResult.Success] with a [PagedResult] of [UserInternal] or an error.
     */
    public suspend fun getUsersInternalPage(
        managementUserPermissionCodes: Set<PermissionCode>,
        queryParams: UserListQueryParams
    ): AppResult<PagedResult<UserInternal>>

    /**
     * Restores a user who was previously scheduled for deletion.
     *
     * @param userId The ID of the user to restore.
     * @param newStatus The account status to set after restoration.
     * @return [AppResult.Success] with the restored [UserInternal] or an error.
     */
    public suspend fun restoreUser(
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
    public suspend fun scheduleUserDeletion(
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
    public suspend fun deleteUser(userId: UserId): AppResult<Unit>

    /**
     * Unlocks a temporarily locked user account.
     *
     * @param userId The ID of the user to unlock.
     * @param clearLockoutForIdentifiers Optional list of identifiers (e.g., email, phone) to clear from lockout cache.
     * @return [AppResult.Success] with the updated [UserInternal] or an error.
     */
    public suspend fun unlockUserAccount(
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
    public suspend fun lockUserAccount(
        userId: UserId,
        temporaryLockoutUntil: Instant
    ): AppResult<UserInternal>

    /**
     * Locks a user account indefinitely under an indefinite lockout.
     *
     * @param userId The ID of the user to lock indefinitely.
     * @return [AppResult.Success] with the updated [UserInternal] or an error.
     */
    public suspend fun lockUserAccountIndefinitely(userId: UserId): AppResult<UserInternal>

    /**
     * Identifies and removes users whose scheduled permanent deletion timestamp has passed.
     *
     * Intended for use by background system tasks.
     *
     * @return [AppResult.Success] with the count of deleted rows or an error.
     */
    public suspend fun deleteUsersDueForPermanentDeletion(): AppResult<Int>

    /**
     * Identifies and unlocks user accounts whose temporary lockout timestamp has passed.
     *
     * Intended for use by background system tasks.
     *
     * @return [AppResult.Success] with the count of unlocked accounts or an error.
     */
    public suspend fun unlockExpiredAccountLockouts(): AppResult<Int>
}
