package io.github.mudrichenkoevgeny.backend.feature.user.database.repository.usersession

import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.UserRoleAccessFilter
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientType
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.SortOrder
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserSortValues
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSession
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionInternal
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.token.RefreshTokenHash
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId

/**
 * Persistence API for refresh sessions created by the user feature.
 */
interface UserSessionRepository {
    /**
     * Persists a new session record.
     *
     * @param userSession session to create
     * @return created session or an error
     */
    suspend fun createUserSession(userSession: UserSessionInternal): AppResult<UserSessionInternal>

    /**
     * Deletes a session by session id.
     *
     * @param userSessionId session id
     * @return success or an error
     */
    suspend fun deleteUserSessionById(userSessionId: UserSessionId): AppResult<Unit>

    /**
     * Deletes all sessions for a given user.
     *
     * @param userId user id
     * @return success or an error
     */
    suspend fun deleteAllUserSessions(userId: UserId): AppResult<Unit>

    /**
     * Deletes all sessions for a user except the one specified by [userSessionId].
     *
     * @param userId user id
     * @param userSessionId session id to keep
     * @return success or an error
     */
    suspend fun deleteAllUserSessionsExceptOne(
        userId: UserId,
        userSessionId: UserSessionId
    ): AppResult<List<UserSessionId>>

    /**
     * Deletes the least recently used (LRU) session for a specific user.
     * The target session is determined by the oldest [UserSession.lastAccessedAt] timestamp.
     * Used to free up space when the user exceeds the maximum allowed number of concurrent sessions.
     *
     * @param userId user id
     * @return the ID of the deleted session or an error
     */
    suspend fun deleteLeastRecentlyUsedUserSession(userId: UserId): AppResult<UserSessionId>

    /**
     * Updates the "last accessed" timestamp for a session.
     *
     * @param userSessionId session id
     * @return success or an error
     */
    suspend fun updateLastAccessed(userSessionId: UserSessionId): AppResult<Unit>

    /**
     * Updates the "last reauthenticated" timestamp for a session.
     *
     * @param userSessionId session id
     * @return success or an error
     */
    suspend fun updateLastReauthenticated(userSessionId: UserSessionId): AppResult<Unit>

    /**
     * Loads a session by id.
     *
     * @param userSessionId session id
     * @return session when found, `null` when missing, or an error
     */
    suspend fun getUserSessionInternalById(userSessionId: UserSessionId): AppResult<UserSessionInternal?>

    /**
     * Loads a session by id.
     *
     * @param userSessionId session id
     * @return session when found, `null` when missing, or an error
     */
    suspend fun getUserSessionById(userSessionId: UserSessionId): AppResult<UserSession?>

    /**
     * Loads a session by refresh token hash and an optional user filter.
     *
     * @param userId optional user id filter
     * @param refreshTokenHash refresh token hash
     * @return session when found, `null` when missing, or an error
     */
    suspend fun getUserSessionByHash(
        userId: UserId?,
        refreshTokenHash: RefreshTokenHash
    ): AppResult<UserSessionInternal?>

    /**
     * Loads all sessions for a user.
     *
     * @param userId user id
     * @return list of sessions or an error
     */
    suspend fun getAllUserSessions(userId: UserId): AppResult<List<UserSessionInternal>>

    /**
     * Loads sessions for the given identifier id with an optional owner filter.
     *
     * @param userIdentifierId identifier id used by sessions
     * @param userId optional owner filter for user-scoped access
     * @return list of sessions or an error
     */
    suspend fun getUserSessionsByIdentifierId(
        userIdentifierId: UserIdentifierId,
        userId: UserId? = null
    ): AppResult<List<UserSessionInternal>>

    /**
     * Returns a paginated list of user sessions filtered by target user roles and technical metadata.
     * This method performs a cross-table check using [accessFilter] to restrict results
     * to sessions belonging to users within the allowed administrative scope.
     *
     * @param accessFilter row-level visibility based on user roles
     * @param pageParams pagination settings (page index and size)
     * @param sortBy field to sort by
     * @param sortOrder sorting direction
     * @param userIds optional filters for specific user IDs
     * @param userRoles optional filters for specific user roles
     * @param identifiers optional filters for identifier values (e.g., emails) associated with the session
     * @param identifierIds optional filters for specific user identifier IDs
     * @param identifierAuthProviders optional filters for authentication providers
     * @param clientTypes optional filter by client platform types (Web, Mobile, etc.)
     * @param userAgents optional filters for browser/client user-agent strings
     * @param ipAddresses optional filters for client IP addresses
     * @param languages optional filters for client language headers
     * @param deviceIds optional filters for unique hardware device IDs
     * @param deviceNames optional filters for human-readable device names
     * @param appVersions optional filters for specific application versions
     * @param operationSystemVersions optional filters for OS versions
     * @return paged result or a database error
     */
    suspend fun getUserSessionsPageWithAccessFilter(
        accessFilter: UserRoleAccessFilter = UserRoleAccessFilter(emptySet()),
        pageParams: PageParams,
        sortBy: UserSortValues.UserSessionSortBy = UserSortValues.UserSessionSortBy.CREATED_AT,
        sortOrder: SortOrder = SortOrder.DESC,
        userIds: List<UserId> = emptyList(),
        userRoles: List<UserRole> = emptyList(),
        identifiers: List<String> = emptyList(),
        identifierIds: List<UserIdentifierId> = emptyList(),
        identifierAuthProviders: List<UserAuthProvider> = emptyList(),
        clientTypes: List<ClientType> = emptyList(),
        userAgents: List<String> = emptyList(),
        ipAddresses: List<String> = emptyList(),
        languages: List<String> = emptyList(),
        deviceIds: List<String> = emptyList(),
        deviceNames: List<String> = emptyList(),
        appVersions: List<String> = emptyList(),
        operationSystemVersions: List<String> = emptyList()
    ): AppResult<PagedResult<UserSession>>

    /**
     * Returns a paginated list of sessions belonging to a specific user.
     * Optimized for direct owner access to their session history, bypassing role-based visibility checks.
     *
     * @param userId the owner of the sessions
     * @param pageParams pagination settings (page index and size)
     * @param sortBy field to sort by
     * @param sortOrder sorting direction
     * @param identifiers optional filters for identifier values associated with the session
     * @param identifierIds optional filters for specific user identifier IDs
     * @param identifierAuthProviders optional filters for authentication providers
     * @param clientTypes optional filter by client platform types
     * @param userAgents optional filters for client user-agent strings
     * @param ipAddresses optional filters for client IP addresses
     * @param languages optional filters for client language headers
     * @param deviceIds optional filters for unique hardware device IDs
     * @param deviceNames optional filters for device names
     * @param appVersions optional filters for application versions
     * @param operationSystemVersions optional filters for OS versions
     * @return paged result of the user's sessions or a database error
     */
    suspend fun getUserSessionsPageByUserId(
        userId: UserId,
        pageParams: PageParams,
        sortBy: UserSortValues.UserSessionSortBy = UserSortValues.UserSessionSortBy.CREATED_AT,
        sortOrder: SortOrder = SortOrder.DESC,
        identifiers: List<String> = emptyList(),
        identifierIds: List<UserIdentifierId> = emptyList(),
        identifierAuthProviders: List<UserAuthProvider> = emptyList(),
        clientTypes: List<ClientType> = emptyList(),
        userAgents: List<String> = emptyList(),
        ipAddresses: List<String> = emptyList(),
        languages: List<String> = emptyList(),
        deviceIds: List<String> = emptyList(),
        deviceNames: List<String> = emptyList(),
        appVersions: List<String> = emptyList(),
        operationSystemVersions: List<String> = emptyList()
    ): AppResult<PagedResult<UserSession>>
}