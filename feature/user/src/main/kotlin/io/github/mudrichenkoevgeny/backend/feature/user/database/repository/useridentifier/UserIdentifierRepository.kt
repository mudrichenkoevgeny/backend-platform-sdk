package io.github.mudrichenkoevgeny.backend.feature.user.database.repository.useridentifier

import io.github.mudrichenkoevgeny.backend.core.common.listing.pagination.model.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.listing.pagination.model.PagedResponse
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.model.UserId
import io.github.mudrichenkoevgeny.backend.feature.user.model.useridentifier.UserIdentifier
import io.github.mudrichenkoevgeny.backend.core.common.model.UserIdentifierId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.UserAuthProvider

/**
 * Persistence API for login identifiers linked to a user (email, phone, external provider id).
 */
interface UserIdentifierRepository {
    /**
     * Persists a new user identifier entry.
     *
     * @param userIdentifier identifier model to create
     * @return created identifier or an error
     */
    suspend fun createUserIdentifier(userIdentifier: UserIdentifier): AppResult<UserIdentifier>

    /**
     * Deletes an identifier entry by id.
     *
     * @param userIdentifierId identifier id to delete
     * @return success or an error
     */
    suspend fun deleteUserIdentifier(userIdentifierId: UserIdentifierId): AppResult<Unit>

    /**
     * Deletes all identifiers for a given user.
     *
     * @param userId user id whose identifiers should be removed
     * @return success or an error
     */
    suspend fun deleteAllUserIdentifierByUserId(userId: UserId): AppResult<Unit>

    /**
     * Updates selected fields of the provided [userIdentifier].
     *
     * When all optional update fields are `null`, returns the original [userIdentifier] without touching storage.
     *
     * @param userIdentifier current identifier snapshot
     * @param identifier optional identifier value override
     * @param passwordHash optional password hash override
     * @return updated identifier snapshot or an error
     */
    suspend fun updateUserIdentifier(
        userIdentifier: UserIdentifier,
        identifier: String? = null,
        passwordHash: String? = null
    ): AppResult<UserIdentifier>

    /**
     * Loads an identifier by id.
     *
     * @param userIdentifierId identifier id to look up
     * @return identifier when found, `null` when missing, or an error
     */
    suspend fun getUserIdentifierById(userIdentifierId: UserIdentifierId): AppResult<UserIdentifier?>

    /**
     * Returns identifiers for a user, optionally filtered by [userAuthProvider].
     *
     * @param userId user id
     * @param userAuthProvider optional auth provider filter
     * @return list of identifiers or an error
     */
    suspend fun getUserIdentifiersListByUserId(
        userId: UserId,
        userAuthProvider: UserAuthProvider? = null
    ): AppResult<List<UserIdentifier>>

    /**
     * Loads a single identifier by provider and identifier value.
     *
     * @param userAuthProvider provider type
     * @param identifier identifier value (email, phone number, external id)
     * @return identifier when found, `null` when missing, or an error
     */
    suspend fun getUserIdentifier(
        userAuthProvider: UserAuthProvider,
        identifier: String,
    ): AppResult<UserIdentifier?>

    /**
     * Returns a paginated list of identifiers with an optional provider filter.
     *
     * @param params pagination parameters
     * @param userAuthProvider optional auth provider filter
     * @return paged response or an error
     */
    suspend fun getUserIdentifiersList(
        params: PageParams,
        userAuthProvider: UserAuthProvider? = null
    ): AppResult<PagedResponse<UserIdentifier>>
}