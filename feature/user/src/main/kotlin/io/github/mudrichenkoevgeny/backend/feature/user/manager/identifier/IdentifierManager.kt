package io.github.mudrichenkoevgeny.backend.feature.user.manager.identifier

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.SortOrder
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.permission.PermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifier
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
    suspend fun getUserIdentifierInternalByProvider(
        userAuthProvider: UserAuthProvider,
        identifier: String
    ): AppResult<UserIdentifierInternal?>

    suspend fun getUserIdentifierByIdForSystem(
        userIdentifierId: UserIdentifierId
    ): AppResult<UserIdentifierInternal?>
    /**
     * Loads an identifier by id with permission-aware masking.
     *
     * [managementUserPermissionCodes] define which target roles are accessible and whether sensitive fields
     * should be returned masked or unmasked.
     *
     * @param userIdentifierId identifier id
     * @param managementUserId caller user id (used for permission errors)
     * @param managementUserPermissionCodes caller permissions
     * @return identifier when found and allowed, `null` when missing, or an error
     */
    suspend fun getUserIdentifierByIdForManagement(
        userIdentifierId: UserIdentifierId,
        managementUserId: UserId,
        managementUserPermissionCodes: Set<PermissionCode>
    ): AppResult<UserIdentifier?>

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
        password: String? = null,
        externalProviderEmail: String? = null
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
     * @param password new password to hash
     * @return updated identifier snapshot or an error
     */
    suspend fun updateUserIdentifierPassword(
        userIdentifier: UserIdentifierInternal,
        password: String
    ): AppResult<UserIdentifier>

    /**
     * Returns a paginated list of identifiers for management purposes.
     * * Applies role-scoped visibility and data masking based on [managementUserPermissionCodes].
     * This method ensures that the caller can only see identifiers of users within their
     * allowed administrative scope.
     *
     * @param managementUserPermissionCodes caller permissions used to derive access and masking rules
     * @param pageParams pagination settings (page, size)
     * @param sortBy field to sort by
     * @param sortOrder sorting direction
     * @param userIds optional filters for specific user IDs
     * @param userAuthProviders optional filters for authentication providers
     * @param identifiers optional substring patterns for identifier values
     * @return paged result with potentially masked data or an error
     */
    suspend fun getIdentifiersPageForManagement(
        managementUserPermissionCodes: Set<PermissionCode>,
        pageParams: PageParams,
        sortBy: UserSortValues.UserIdentifierSortBy = UserSortValues.UserIdentifierSortBy.CREATED_AT,
        sortOrder: SortOrder = SortOrder.DESC,
        userIds: List<UserId> = emptyList(),
        userAuthProviders: List<UserAuthProvider> = emptyList(),
        identifiers: List<String> = emptyList()
    ): AppResult<PagedResult<UserIdentifier>>

    /**
     * Returns a paginated list of identifiers belonging to the current user.
     * * Designed for "Self" service scenarios where the user manages their own
     * authentication methods. No administrative access filters are applied.
     *
     * @param userId the ID of the user requesting their own identifiers
     * @param pageParams pagination settings (page, size)
     * @param sortBy field to sort by
     * @param sortOrder sorting direction
     * @param userAuthProviders optional filters for authentication providers
     * @param identifiers optional substring patterns for identifier values
     * @return paged result of user's own identifiers or an error
     */
    suspend fun getIdentifiersPageForSelf(
        userId: UserId,
        pageParams: PageParams,
        sortBy: UserSortValues.UserIdentifierSortBy = UserSortValues.UserIdentifierSortBy.CREATED_AT,
        sortOrder: SortOrder = SortOrder.DESC,
        userAuthProviders: List<UserAuthProvider> = emptyList(),
        identifiers: List<String> = emptyList()
    ): AppResult<PagedResult<UserIdentifier>>
}