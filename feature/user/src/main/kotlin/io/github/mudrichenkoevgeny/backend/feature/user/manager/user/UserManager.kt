package io.github.mudrichenkoevgeny.backend.feature.user.manager.user

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserDetails
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.permission.UserPermissionCode

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
     * @return created user or an error
     */
    suspend fun createUser(
        role: UserRole,
        accountStatus: UserAccountStatus,
        permissions: Set<UserPermissionCode>
    ): AppResult<UserDetails>

    /**
     * Loads an existing user by [userId] or creates a new user when [userId] is `null`.
     *
     * @param userId optional user id to load
     * @param role role used when creating a new user
     * @param accountStatus status used when creating a new user
     * @return loaded or created user, or an error
     */
    suspend fun getOrCreateUser(
        userId: UserId? = null,
        role: UserRole = UserRole.USER,
        accountStatus: UserAccountStatus = UserAccountStatus.ACTIVE
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

    suspend fun deleteUsersDueForPermanentDeletion(): AppResult<Int>
}