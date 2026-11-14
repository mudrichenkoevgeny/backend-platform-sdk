package io.github.mudrichenkoevgeny.backend.feature.user.database.repository.usersession

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.UserRoleAccessFilter
import io.github.mudrichenkoevgeny.backend.feature.user.network.model.ManagementSessionListQueryParams
import io.github.mudrichenkoevgeny.backend.feature.user.network.model.SelfSessionListQueryParams
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionInternal
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.token.RefreshTokenHash
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId

/**
 * Persistence API for refresh sessions created by the user feature.
 */
public interface UserSessionRepository {
    /**
     * Persists a new session record.
     *
     * @param userSession session to create
     * @return created session or an error
     */
    public suspend fun createUserSession(userSession: UserSessionInternal): AppResult<UserSessionInternal>

    /**
     * Deletes a session by session id.
     *
     * @param userSessionId session id
     * @return success or an error
     */
    public suspend fun deleteUserSessionById(userSessionId: UserSessionId): AppResult<Unit>

    /**
     * Deletes all sessions for a given user.
     *
     * @param userId user id
     * @return success or an error
     */
    public suspend fun deleteAllUserSessions(userId: UserId): AppResult<Unit>

    /**
     * Deletes all sessions for a user except the one specified by [userSessionId].
     *
     * @param userId user id
     * @param userSessionId session id to keep
     * @return success or an error
     */
    public suspend fun deleteAllUserSessionsExceptOne(
        userId: UserId,
        userSessionId: UserSessionId
    ): AppResult<List<UserSessionId>>

    /**
     * Deletes the least recently used (LRU) session for a specific user.
     * Used to free up space when the user exceeds the maximum allowed number of concurrent sessions.
     *
     * @param userId user id
     * @return the ID of the deleted session or an error
     */
    public suspend fun deleteLeastRecentlyUsedUserSession(userId: UserId): AppResult<UserSessionId>

    /**
     * Updates the "last accessed" timestamp for a session.
     *
     * @param userSessionId session id
     * @return success or an error
     */
    public suspend fun updateLastAccessed(userSessionId: UserSessionId): AppResult<Unit>

    /**
     * Updates the "last reauthenticated" timestamp for a session.
     *
     * @param userSessionId session id
     * @return success or an error
     */
    public suspend fun updateLastReauthenticated(userSessionId: UserSessionId): AppResult<Unit>

    /**
     * Loads a session by id.
     *
     * @param userSessionId session id
     * @return session when found, `null` when missing, or an error
     */
    public suspend fun getUserSessionInternalById(userSessionId: UserSessionId): AppResult<UserSessionInternal?>

    /**
     * Loads a session by refresh token hash.
     *
     * @param refreshTokenHash refresh token hash
     * @return session when found, `null` when missing, or an error
     */
    public suspend fun getUserSessionByHash(refreshTokenHash: RefreshTokenHash): AppResult<UserSessionInternal?>

    /**
     * Loads all sessions for a user.
     *
     * @param userId user id
     * @return list of sessions or an error
     */
    public suspend fun getAllUserSessions(userId: UserId): AppResult<List<UserSessionInternal>>

    /**
     * Loads sessions for the given identifier id with an optional owner filter.
     *
     * @param userIdentifierId identifier id used by sessions
     * @param userId optional owner filter for user-scoped access
     * @return list of sessions or an error
     */
    public suspend fun getUserSessionsByIdentifierId(
        userIdentifierId: UserIdentifierId,
        userId: UserId? = null
    ): AppResult<List<UserSessionInternal>>

    /**
     * Returns a paginated list of user sessions filtered by target user roles and technical metadata.
     * This method performs a cross-table check using [accessFilter] to restrict results
     * to sessions belonging to users within the allowed administrative scope.
     *
     * @param accessFilter row-level visibility based on user roles
     * @param queryParams query parameters for filtering and paginating user sessions (such as page settings, user IDs, roles, metadata, and client info)
     * @return paged result or a database error
     */
    public suspend fun getUserSessionsPageWithAccessFilter(
        accessFilter: UserRoleAccessFilter = UserRoleAccessFilter(emptySet()),
        queryParams: ManagementSessionListQueryParams
    ): AppResult<PagedResult<UserSessionInternal>>

    /**
     * Returns a paginated list of sessions belonging to a specific user.
     */
    public suspend fun getUserSessionsPageByUserId(
        userId: UserId,
        queryParams: SelfSessionListQueryParams
    ): AppResult<PagedResult<UserSessionInternal>>
}
