package io.github.mudrichenkoevgeny.backend.feature.user.manager.session

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientInfo
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSession
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.token.RefreshToken
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.token.SessionToken
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import java.time.Instant

/**
 * Manages refresh sessions for the user feature.
 *
 * Responsible for issuing session tokens, persisting refresh sessions, and performing refresh/revoke operations.
 */
interface SessionManager {
    /**
     * Creates a new session for the given user and identifier.
     *
     * @param userId user id
     * // todo doc
     * @param userIdentifierId identifier id used to authenticate
     * @param userIdentifierAuthProvider auth provider used to authenticate
     * @param clientInfo client metadata to bind to the created session
     * @param lastReauthenticatedAt timestamp used to track re-authentication requirements
     * @return session token or an error
     */
    suspend fun createSession(
        userId: UserId,
        userRole: UserRole,
        userIdentifierId: UserIdentifierId,
        userIdentifierAuthProvider: UserAuthProvider,
        clientInfo: ClientInfo,
        lastReauthenticatedAt: Instant
    ): AppResult<SessionToken>

    /**
     * Refreshes a session token using a refresh token.
     *
     * @param userId optional user id filter (when refresh is performed in an authenticated context)
     * @param refreshToken refresh token
     * @param clientInfo client metadata used for session validation
     * @return new session token or an error
     */
    suspend fun refreshSession(
        userId: UserId?,
        refreshToken: RefreshToken,
        clientInfo: ClientInfo
    ): AppResult<SessionToken>

    /**
     * Revokes a session by id.
     *
     * @param userSessionId session id
     * @return success or an error
     */
    suspend fun revokeSessionById(userSessionId: UserSessionId): AppResult<Unit>

    /**
     * Revokes a session by user id and refresh token.
     *
     * @param userId user id
     * @param refreshToken refresh token
     * @return success or an error
     */
    suspend fun revokeSession(userId: UserId, refreshToken: RefreshToken): AppResult<Unit>

    /**
     * Revokes multiple sessions by id.
     *
     * @param userId user id
     * @param sessionIds session ids to revoke
     * @return success or an error
     */
    suspend fun revokeMultipleUserSessions(userId: UserId, sessionIds: List<UserSessionId>): AppResult<Unit>

    /**
     * Revokes all sessions for a user.
     *
     * @param userId user id
     * @return success or an error
     */
    suspend fun revokeAllUserSessions(userId: UserId): AppResult<Unit>

    /**
     * Revokes all sessions for a user except the one specified by [userSessionId].
     *
     * @param userId user id
     * @param userSessionId session id to keep
     * @return success or an error
     */
    suspend fun revokeAllUserSessionsExceptOne(userId: UserId, userSessionId: UserSessionId): AppResult<Unit>

    /**
     * Loads a session by id.
     *
     * @param userSessionId session id
     * @return session when found, `null` when missing, or an error
     */
    suspend fun getUserSessionById(userSessionId: UserSessionId): AppResult<UserSession?>

    /**
     * Loads all sessions for a user.
     *
     * @param userId user id
     * @return list of sessions or an error
     */
    suspend fun getAllUserSessions(userId: UserId): AppResult<List<UserSession>>
}