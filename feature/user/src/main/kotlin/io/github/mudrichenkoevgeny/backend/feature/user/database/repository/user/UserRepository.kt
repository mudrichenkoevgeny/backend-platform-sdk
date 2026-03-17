package io.github.mudrichenkoevgeny.backend.feature.user.database.repository.user

import io.github.mudrichenkoevgeny.backend.core.common.listing.pagination.model.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.listing.pagination.model.PagedResponse
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.model.user.User
import io.github.mudrichenkoevgeny.backend.core.common.model.UserId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.UserRole
import java.time.Instant

/**
 * User persistence API backed by the user feature database schema.
 */
interface UserRepository {
    /**
     * Persists a new [user].
     *
     * @param user user to create
     * @return created user or an error when persistence fails
     */
    suspend fun createUser(user: User): AppResult<User>

    /**
     * Deletes a user by id.
     *
     * @param userId user id to delete
     * @return success or an error when deletion fails
     */
    suspend fun deleteUser(userId: UserId): AppResult<Unit>

    /**
     * Updates selected fields of the provided [user].
     *
     * When all optional update fields are `null`, returns the original [user] without touching storage.
     *
     * @param user current user snapshot
     * @param status optional account status override
     * @param lastLoginAt optional last login timestamp
     * @param lastActiveAt optional last active timestamp
     * @return updated user snapshot or an error when update fails
     */
    suspend fun updateUser(
        user: User,
        status: UserAccountStatus? = null,
        lastLoginAt: Instant? = null,
        lastActiveAt: Instant? = null
    ): AppResult<User>

    /**
     * Loads a user by id.
     *
     * @param userId user id to look up
     * @return user when found, `null` when missing, or an error
     */
    suspend fun getUserById(userId: UserId): AppResult<User?>

    /**
     * Returns a paginated list of users with optional filters.
     *
     * @param params pagination parameters
     * @param role optional role filter
     * @param accountStatus optional account status filter
     * @return paged response or an error
     */
    suspend fun getUsersList(
        params: PageParams,
        role: UserRole? = null,
        accountStatus: UserAccountStatus? = null,
    ): AppResult<PagedResponse<User>>
}