package io.github.mudrichenkoevgeny.backend.feature.user.manager.useridentifier

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.model.UserId
import io.github.mudrichenkoevgeny.backend.core.common.model.UserIdentifierId
import io.github.mudrichenkoevgeny.backend.feature.user.model.useridentifier.UserIdentifier
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.UserAuthProvider

/**
 * Manages user identifiers (login methods) for the user feature.
 *
 * Provides a higher-level API over the identifier repository and password hashing.
 */
interface UserIdentifierManager {
    /**
     * Loads an identifier by provider and identifier value.
     *
     * @param userAuthProvider provider type
     * @param identifier identifier value (email/phone/external subject)
     * @return identifier when found, `null` when missing, or an error
     */
    suspend fun getUserIdentifier(
        userAuthProvider: UserAuthProvider,
        identifier: String,
    ): AppResult<UserIdentifier?>

    /**
     * Returns all identifiers for a user.
     *
     * @param userId user id
     * @return list of identifiers or an error
     */
    suspend fun getUserIdentifierListByUserId(
        userId: UserId
    ): AppResult<List<UserIdentifier>>

    /**
     * Creates a new identifier for a user.
     *
     * Hashes [password] when provided.
     *
     * @param userId user id
     * @param userAuthProvider provider type
     * @param identifier identifier value
     * @param password optional password for password-based providers
     * @return created identifier or an error
     */
    suspend fun createUserIdentifier(
        userId: UserId,
        userAuthProvider: UserAuthProvider,
        identifier: String,
        password: String? = null
    ): AppResult<UserIdentifier>

    /**
     * Deletes an identifier by id.
     *
     * @param userIdentifierId identifier id
     * @return success or an error
     */
    suspend fun deleteUserIdentifier(
        userIdentifierId: UserIdentifierId
    ): AppResult<Unit>

    /**
     * Updates the identifier value and password for an existing identifier.
     *
     * @param userIdentifier current identifier snapshot
     * @param identifier new identifier value
     * @param password new password to hash
     * @return updated identifier snapshot or an error
     */
    suspend fun updateUserIdentifierPassword(
        userIdentifier: UserIdentifier,
        identifier: String,
        password: String
    ): AppResult<UserIdentifier>
}