package io.github.mudrichenkoevgeny.backend.feature.user.database.repository.usersession

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.model.auth.RefreshTokenHash
import io.github.mudrichenkoevgeny.backend.core.common.model.UserId
import io.github.mudrichenkoevgeny.backend.feature.user.model.session.UserSession
import io.github.mudrichenkoevgeny.backend.core.common.model.UserSessionId

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
    suspend fun createUserSession(userSession: UserSession): AppResult<UserSession>

    /**
     * Deletes a session by user id and refresh token hash.
     *
     * @param userId user id
     * @param refreshTokenHash refresh token hash to match
     * @return success or an error
     */
    suspend fun deleteUserSession(userId: UserId, refreshTokenHash: RefreshTokenHash): AppResult<Unit>

    /**
     * Deletes a session by session id.
     *
     * @param userSessionId session id
     * @return success or an error
     */
    suspend fun deleteUserSessionById(userSessionId: UserSessionId): AppResult<Unit>

    /**
     * Deletes multiple sessions by id for a given user.
     *
     * @param userId user id
     * @param sessionIds session ids to delete
     * @return success or an error
     */
    suspend fun deleteMultipleUserSessions(userId: UserId, sessionIds: List<UserSessionId>): AppResult<Unit>

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
    suspend fun deleteAllUserSessionsExceptOne(userId: UserId, userSessionId: UserSessionId): AppResult<Unit>

    /**
     * Updates the "last accessed" timestamp for a session.
     *
     * @param userSessionId session id
     * @return success or an error
     */
    suspend fun updateLastAccessed(userSessionId: UserSessionId): AppResult<Unit>

    /**
     * Marks a session as revoked by refresh token hash.
     *
     * @param refreshTokenHash refresh token hash
     * @return success or an error
     */
    suspend fun revokeSession(refreshTokenHash: RefreshTokenHash): AppResult<Unit>

    /**
     * Revokes all non-revoked sessions for a user.
     *
     * @param userId user id
     * @return success or an error
     */
    suspend fun revokeAllSessionsForUser(userId: UserId): AppResult<Unit>

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
    suspend fun getUserSessionByHash(userId: UserId?, refreshTokenHash: RefreshTokenHash): AppResult<UserSession?>

    /**
     * Loads all sessions for a user.
     *
     * @param userId user id
     * @return list of sessions or an error
     */
    suspend fun getAllUserSessions(userId: UserId): AppResult<List<UserSession>>
}