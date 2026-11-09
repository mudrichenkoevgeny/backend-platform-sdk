package io.github.mudrichenkoevgeny.backend.feature.user.database.repository.user

import io.github.mudrichenkoevgeny.backend.core.common.model.UpdateField
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.UserRoleAccessFilter
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.user.UpdateUserParams
import io.github.mudrichenkoevgeny.backend.feature.user.network.model.UserListQueryParams
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserInternal
import kotlin.time.Instant

/**
 * Data access for users (persistence and querying).
 *
 * Implementations are responsible for storing user rows and returning paginated lists
 * with optional filters.
 */
public interface UserRepository {
    /**
     * Persists a new user record.
     *
     * @param user user internal model to create.
     * @return [AppResult] containing the created [UserInternal] or an error.
     */
    public suspend fun createUser(user: UserInternal): AppResult<UserInternal>

    /**
     * Deletes a user record by the unique user identifier.
     *
     * @param userId unique user identifier.
     * @return [AppResult] containing [Unit] on success or an error.
     */
    public suspend fun deleteUser(userId: UserId): AppResult<Unit>

    /**
     * Updates user account fields using [updateUserParams].
     *
     * Each property inside [updateUserParams] uses [UpdateField]
     * to distinguish between ignoring, setting a value, or explicitly clearing a field.
     *
     * @param userId unique identifier of the user to update.
     * @param updateUserParams structured holder containing all field update parameters.
     * @return [AppResult] containing the updated [UserInternal].
     */
    public suspend fun updateUser(
        userId: UserId,
        updateUserParams: UpdateUserParams
    ): AppResult<UserInternal>

    /**
     * Retrieves user internal model by nickname (case-insensitive).
     *
     * @param nickname user nickname string.
     * @return [AppResult] containing [UserInternal] when found, `null` when missing, or an error.
     */
    public suspend fun getUserInternalByNickname(nickname: String): AppResult<UserInternal?>

    /**
     * Retrieves user internal model by the unique user identifier.
     *
     * @param userId unique user identifier.
     * @return [AppResult] containing [UserInternal] when found, `null` when missing, or an error.
     */
    public suspend fun getUserInternalById(userId: UserId): AppResult<UserInternal?>

    /**
     * Returns a page of [UserInternal] rows matching optional filters in [queryParams].
     *
     * @param accessFilter Row-level role visibility applied before optional filters.
     * @param queryParams Structured filter and pagination parameters.
     * @return [PagedResult] of matching users or an error.
     */
    public suspend fun getUsersPageWithAccessFilter(
        accessFilter: UserRoleAccessFilter = UserRoleAccessFilter(emptySet()),
        queryParams: UserListQueryParams
    ): AppResult<PagedResult<UserInternal>>

    /**
     * Deletes users whose scheduled permanent deletion time has passed relative to [asOf].
     *
     * A row matches when [UserInternal.scheduledPermanentDeletionAt] is non-null and
     * `scheduledPermanentDeletionAt <= asOf`.
     *
     * @param asOf Upper bound instant for eligibility (inclusive).
     * @return Count of deleted rows, or an error when persistence fails.
     */
    public suspend fun deleteUsersDueForPermanentDeletion(asOf: Instant): AppResult<Int>

    /**
     * Finds users with an active temporary lockout whose [UserInternal.temporaryLockoutUntil] has passed relative to [asOf].
     *
     * @param asOf Upper bound instant for lockout expiration (inclusive).
     * @return List of matching [UserId]s, or an error.
     */
    public suspend fun getExpiredTemporaryLockouts(asOf: Instant): AppResult<List<UserId>>

    /**
     * Unlocks temporary account lockouts for a batch of users in a single SQL operation.
     *
     * @param userIds list of user IDs to unlock.
     * @return [AppResult] containing [Unit] on success or an error.
     */
    public suspend fun unlockUsersBatch(userIds: List<UserId>): AppResult<Unit>
}
