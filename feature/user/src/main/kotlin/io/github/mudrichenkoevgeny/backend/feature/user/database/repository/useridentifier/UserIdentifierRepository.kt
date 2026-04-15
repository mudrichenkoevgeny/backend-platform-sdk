package io.github.mudrichenkoevgeny.backend.feature.user.database.repository.useridentifier

import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.UserRoleAccessFilter
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.SortOrder
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierInternal
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserSortValues
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId

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
    suspend fun createUserIdentifier(userIdentifier: UserIdentifierInternal): AppResult<UserIdentifierInternal>

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
        userIdentifier: UserIdentifierInternal,
        identifier: String? = null,
        passwordHash: String? = null
    ): AppResult<UserIdentifierInternal>

    /**
     * Loads an identifier by id.
     *
     * @param userIdentifierId identifier id to look up
     * @return identifier when found, `null` when missing, or an error
     */
    suspend fun getUserIdentifierById(userIdentifierId: UserIdentifierId): AppResult<UserIdentifierInternal?>

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
    ): AppResult<List<UserIdentifierInternal>>

    /**
     * Loads a single identifier by provider and identifier value.
     *
     * @param userAuthProvider provider type
     * @param identifier identifier value (email, phone number, external id)
     * @return identifier when found, `null` when missing, or an error
     */
    suspend fun getUserIdentifier(
        userAuthProvider: UserAuthProvider,
        identifier: String
    ): AppResult<UserIdentifierInternal?>

    /**
     * Returns a paginated list of identifiers constrained by access and optional filters.
     *
     * [accessFilter] limits visible rows by target user roles.
     * Filter lists are OR-combined within each axis and AND-combined across axes.
     *
     * @param accessFilter row-level role visibility
     * @param params pagination parameters
     * @param sortBy sort field
     * @param sortOrder sort direction
     * @param userIds optional user-id filters
     * @param userAuthProviders optional auth-provider filters
     * @param identifiers optional identifier substring filters
     * @return paged response or an error
     */
    suspend fun getUserIdentifiersList(
        accessFilter: UserRoleAccessFilter = UserRoleAccessFilter(emptySet()),
        params: PageParams,
        sortBy: UserSortValues.UserIdentifierSortBy = UserSortValues.UserIdentifierSortBy.CREATED_AT,
        sortOrder: SortOrder = SortOrder.DESC,
        userIds: List<UserId> = emptyList(),
        userAuthProviders: List<UserAuthProvider> = emptyList(),
        identifiers: List<String> = emptyList()
    ): AppResult<PagedResult<UserIdentifierInternal>>
}