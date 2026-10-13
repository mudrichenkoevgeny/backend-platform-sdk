package io.github.mudrichenkoevgeny.backend.feature.user.manager.identifier

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.SortOrder
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.permission.PermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierInternal
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierPrivate
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierSummary
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserSortValues
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId

/**
 * Manages user identifiers (login methods) for the user feature.
 *
 * Provides a higher-level API over the identifier repository and password hashing.
 */
interface IdentifierManager {
    /**
     * Loads an identifier by provider and identifier value for internal system use.
     *
     * @param userAuthProvider provider type
     * @param identifier identifier value (email/phone/external subject)
     * @return [AppResult.Success] containing [UserIdentifierInternal] when found, `null` when missing, or an error
     */
    suspend fun getUserIdentifierInternalByProvider(
        userAuthProvider: UserAuthProvider,
        identifier: String
    ): AppResult<UserIdentifierInternal?>

    /**
     * Loads an identifier by its ID for internal system use.
     * Returns full internal data including sensitive fields like hashed passwords.
     *
     * @param userIdentifierId identifier id
     * @return [AppResult.Success] containing [UserIdentifierInternal] when found, `null` when missing, or an error
     */
    suspend fun getUserIdentifierInternalById(
        userIdentifierId: UserIdentifierId
    ): AppResult<UserIdentifierInternal?>

    /**
     * Loads an identifier by its ID for the current user's self-management.
     *
     * @param userIdentifierId identifier id
     * @return [AppResult.Success] containing [UserIdentifierPrivate] when found, `null` when missing, or an error
     */
    suspend fun getUserIdentifierPrivateById(
        userIdentifierId: UserIdentifierId
    ): AppResult<UserIdentifierPrivate?>

    /**
     * Loads an identifier by id with permission-aware access checks for management use.
     *
     * [managementUserPermissionCodes] define which target roles are accessible.
     *
     * @param userIdentifierId identifier id
     * @param managementUserId caller user id (used for permission errors)
     * @param managementUserPermissionCodes caller permissions
     * @return [AppResult.Success] containing [UserIdentifierPrivate] when found and allowed, `null` when missing, or an error
     */
    suspend fun getUserIdentifierPrivateById(
        userIdentifierId: UserIdentifierId,
        managementUserId: UserId,
        managementUserPermissionCodes: Set<PermissionCode>
    ): AppResult<UserIdentifierPrivate?>

    /**
     * Returns all internal identifiers belonging to a user.
     *
     * @param userId user id
     * @return [AppResult.Success] with list of [UserIdentifierInternal] or an error
     */
    suspend fun getUserIdentifiersInternalPageByUserId(
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
     * @param externalProviderEmail optional email from an external provider
     * @param externalProviderDisplayName optional display name from an external provider
     * @return [AppResult.Success] with created [UserIdentifierInternal] or an error
     */
    suspend fun createUserIdentifier(
        userId: UserId,
        userAuthProvider: UserAuthProvider,
        identifier: String,
        password: String? = null,
        externalProviderEmail: String? = null,
        externalProviderDisplayName: String? = null
    ): AppResult<UserIdentifierInternal>

    /**
     * Deletes an identifier by id.
     *
     * @param userIdentifierId identifier id
     * @return [AppResult.Success] with [Unit] or an error
     */
    suspend fun deleteUserIdentifier(
        userIdentifierId: UserIdentifierId
    ): AppResult<Unit>

    /**
     * Updates the identifier value and password for an existing identifier.
     *
     * @param userIdentifier current identifier snapshot
     * @param password new password to hash
     * @return [AppResult.Success] with updated [UserIdentifierPrivate] snapshot or an error
     */
    suspend fun updateUserIdentifierPassword(
        userIdentifier: UserIdentifierInternal,
        password: String
    ): AppResult<UserIdentifierPrivate>

    /**
     * Revokes and clears the password hash from the specified user identifier record.
     *
     * @param userIdentifier current identifier snapshot
     * @return [AppResult.Success] with updated [UserIdentifierPrivate] snapshot or an error
     */
    suspend fun clearUserIdentifierPassword(
        userIdentifier: UserIdentifierInternal
    ): AppResult<UserIdentifierPrivate>

    /**
     * Returns a paginated list of identifier summaries for management purposes.
     * Applies role-scoped visibility based on [managementUserPermissionCodes].
     *
     * @param managementUserPermissionCodes caller permissions used to derive access rules
     * @param pageParams pagination settings (page, size)
     * @param sortBy field to sort by
     * @param sortOrder sorting direction
     * @param userIds optional filters for specific user IDs
     * @param userAuthProviders optional filters for authentication providers
     * @param identifiers optional substring patterns for identifier values
     * @return [AppResult.Success] containing [PagedResult] of [UserIdentifierSummary] or an error
     */
    suspend fun getUserIdentifiersSummaryPage(
        managementUserPermissionCodes: Set<PermissionCode>,
        pageParams: PageParams,
        sortBy: UserSortValues.UserIdentifierSortBy = UserSortValues.UserIdentifierSortBy.DEFAULT,
        sortOrder: SortOrder = SortOrder.DEFAULT,
        userIds: List<UserId> = emptyList(),
        userAuthProviders: List<UserAuthProvider> = emptyList(),
        identifiers: List<String> = emptyList()
    ): AppResult<PagedResult<UserIdentifierSummary>>

    /**
     * Returns a paginated list of identifier summaries belonging to the current user.
     *
     * @param userId the ID of the user requesting their own identifiers
     * @param pageParams pagination settings (page, size)
     * @param sortBy field to sort by
     * @param sortOrder sorting direction
     * @param userAuthProviders optional filters for authentication providers
     * @param identifiers optional substring patterns for identifier values
     * @return [AppResult.Success] containing [PagedResult] of [UserIdentifierSummary] or an error
     */
    suspend fun getUserIdentifiersSummaryPage(
        userId: UserId,
        pageParams: PageParams,
        sortBy: UserSortValues.UserIdentifierSortBy = UserSortValues.UserIdentifierSortBy.DEFAULT,
        sortOrder: SortOrder = SortOrder.DEFAULT,
        userAuthProviders: List<UserAuthProvider> = emptyList(),
        identifiers: List<String> = emptyList()
    ): AppResult<PagedResult<UserIdentifierSummary>>
}
