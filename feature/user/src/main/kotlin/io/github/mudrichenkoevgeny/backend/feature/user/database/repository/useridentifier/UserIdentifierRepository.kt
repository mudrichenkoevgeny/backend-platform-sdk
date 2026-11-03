package io.github.mudrichenkoevgeny.backend.feature.user.database.repository.useridentifier

import io.github.mudrichenkoevgeny.backend.core.common.model.UpdateField
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.UserRoleAccessFilter
import io.github.mudrichenkoevgeny.backend.feature.user.network.model.ManagementIdentifierListQueryParams
import io.github.mudrichenkoevgeny.backend.feature.user.network.model.SelfIdentifierListQueryParams
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.passwordhash.PasswordHash
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierInternal
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierId
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
     * Updates selected fields of the provided [userIdentifier].
     *
     * Each parameter uses [UpdateField] to distinguish between:
     * - [UpdateField.Ignore]: Field will not be included in the update.
     * - [UpdateField.Set]: Field will be updated to the provided value (including null).
     *
     * @param userIdentifier current identifier snapshot.
     * @param newPasswordHash new password hash.
     * @return updated identifier snapshot or an error.
     */
    suspend fun updatePasswordHash(
        userIdentifier: UserIdentifierInternal,
        newPasswordHash: PasswordHash?
    ): AppResult<UserIdentifierInternal>

    /**
     * Loads an identifier by id.
     *
     * @param userIdentifierId identifier id to look up
     * @return identifier when found, `null` when missing, or an error
     */
    suspend fun getUserIdentifierInternalById(userIdentifierId: UserIdentifierId): AppResult<UserIdentifierInternal?>

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
    suspend fun getUserIdentifierInternalByProvider(
        userAuthProvider: UserAuthProvider,
        identifier: String
    ): AppResult<UserIdentifierInternal?>

    /**
     * Returns a paginated list of identifiers matching optional filters in [queryParams].
     *
     * This method performs a cross-table check using [accessFilter] to restrict results
     * to identifiers belonging to users matching allowed roles.
     *
     * @param accessFilter row-level visibility based on user roles
     * @param queryParams structured filter and pagination parameters
     * @return paged result of matching identifiers or a database error
     */
    suspend fun getUserIdentifiersPageWithAccessFilter(
        accessFilter: UserRoleAccessFilter = UserRoleAccessFilter(emptySet()),
        queryParams: ManagementIdentifierListQueryParams
    ): AppResult<PagedResult<UserIdentifierInternal>>

    /**
     * Returns a paginated list of identifiers belonging to a specific user matching optional filters in [queryParams].
     *
     * Optimized for direct owner access, bypassing role-based visibility checks.
     *
     * @param userId the owner of the identifiers
     * @param queryParams structured filter and pagination parameters
     * @return paged result of matching identifiers or a database error
     */
    suspend fun getUserIdentifiersPageByUserId(
        userId: UserId,
        queryParams: SelfIdentifierListQueryParams
    ): AppResult<PagedResult<UserIdentifierInternal>>
}
