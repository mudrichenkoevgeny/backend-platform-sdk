package io.github.mudrichenkoevgeny.backend.feature.user.manager.session

import io.github.mudrichenkoevgeny.backend.core.common.network.request.model.ClientInfo
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.model.auth.RefreshToken
import io.github.mudrichenkoevgeny.backend.feature.user.model.auth.SessionToken
import io.github.mudrichenkoevgeny.backend.core.common.model.UserId
import io.github.mudrichenkoevgeny.backend.core.common.model.UserIdentifierId
import io.github.mudrichenkoevgeny.backend.core.common.model.UserSessionId
import io.github.mudrichenkoevgeny.backend.feature.user.enums.UserAuthProvider
import io.github.mudrichenkoevgeny.backend.feature.user.model.session.UserSession
import java.time.Instant

interface SessionManager {
    suspend fun createSession(
        userId: UserId,
        userIdentifierId: UserIdentifierId,
        userIdentifierAuthProvider: UserAuthProvider,
        clientInfo: ClientInfo,
        lastReauthenticatedAt: Instant
    ): AppResult<SessionToken>

    suspend fun refreshSession(
        userId: UserId?,
        refreshToken: RefreshToken,
        clientInfo: ClientInfo
    ): AppResult<SessionToken>

    suspend fun revokeSessionById(userSessionId: UserSessionId): AppResult<Unit>
    suspend fun revokeSession(userId: UserId, refreshToken: RefreshToken): AppResult<Unit>
    suspend fun revokeMultipleUserSessions(userId: UserId, sessionIds: List<UserSessionId>): AppResult<Unit>
    suspend fun revokeAllUserSessions(userId: UserId): AppResult<Unit>
    suspend fun revokeAllUserSessionsExceptOne(userId: UserId, userSessionId: UserSessionId): AppResult<Unit>

    suspend fun getUserSessionById(userSessionId: UserSessionId): AppResult<UserSession?>
    suspend fun getAllUserSessions(userId: UserId): AppResult<List<UserSession>>
}