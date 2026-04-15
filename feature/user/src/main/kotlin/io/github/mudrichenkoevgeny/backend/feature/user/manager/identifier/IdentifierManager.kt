package io.github.mudrichenkoevgeny.backend.feature.user.manager.identifier

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.SortOrder
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.permission.PermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierInternal
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserSortValues
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId

/**
 * Manages user identifiers (login methods) for the user feature.
 *
 * Provides a higher-level API over the identifier repository and password hashing.
 */
interface IdentifierManager {
    /**
     * Loads an identifier by provider and identifier value.
     *
     * @param userAuthProvider provider type
     * @param identifier identifier value (email/phone/external subject)
     * @return identifier when found, `null` when missing, or an error
     */
    suspend fun getUserIdentifier(
        userAuthProvider: UserAuthProvider,
        identifier: String
    ): AppResult<UserIdentifierInternal?>

    /**
     * Loads an identifier by id with permission-aware masking.
     *
     * [userPermissionCodes] define which target roles are accessible and whether sensitive fields
     * should be returned masked or unmasked.
     *
     * @param userIdentifierId identifier id
     * @param userId caller user id (used for permission errors)
     * @param userPermissionCodes caller permissions
     * @return identifier when found and allowed, `null` when missing, or an error
     */
    suspend fun getUserIdentifierById(
        userIdentifierId: UserIdentifierId,
        userId: UserId,
        userPermissionCodes: Set<PermissionCode>
    ): AppResult<UserIdentifierInternal?>

    /**
     * Returns all identifiers for a user.
     *
     * @param userId user id
     * @return list of identifiers or an error
     */
    suspend fun getUserIdentifiersByUserId(
        userId: UserId
    ): AppResult<List<UserIdentifierInternal>>

    /**
     * Returns a paginated list of identifiers visible for the caller permissions.
     *
     * Role-scoped visibility and masked/unmasked output are derived from [userPermissionCodes].
     * Optional filters are OR-combined within each list and AND-combined across different filter axes.
     *
     * @param userPermissionCodes caller permissions used for access control
     * @param pageParams one-based page and size
     * @param sortBy list sort field
     * @param sortOrder sort direction
     * @param userIds optional user-id filters
     * @param userAuthProviders optional auth-provider filters
     * @param identifiers optional identifier substring filters
     */
    suspend fun getUserIdentifiersList(
        userPermissionCodes: Set<PermissionCode>,
        pageParams: PageParams,
        sortBy: UserSortValues.UserIdentifierSortBy = UserSortValues.UserIdentifierSortBy.CREATED_AT,
        sortOrder: SortOrder = SortOrder.DESC,
        userIds: List<UserId> = emptyList(),
        userAuthProviders: List<UserAuthProvider> = emptyList(),
        identifiers: List<String> = emptyList()
    ): AppResult<PagedResult<UserIdentifierInternal>>

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
    ): AppResult<UserIdentifierInternal>

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
        userIdentifier: UserIdentifierInternal,
        identifier: String, // todo do we need it?
        password: String
    ): AppResult<UserIdentifierInternal>
}