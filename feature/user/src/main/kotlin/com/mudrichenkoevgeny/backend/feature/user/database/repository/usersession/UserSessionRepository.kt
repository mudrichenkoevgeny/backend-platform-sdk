package com.mudrichenkoevgeny.backend.feature.user.database.repository.usersession

import com.mudrichenkoevgeny.backend.core.common.result.AppResult
import com.mudrichenkoevgeny.backend.feature.user.model.auth.RefreshTokenHash
import com.mudrichenkoevgeny.backend.core.common.model.UserId
import com.mudrichenkoevgeny.backend.feature.user.model.session.UserSession
import com.mudrichenkoevgeny.backend.core.common.model.UserSessionId

interface UserSessionRepository {
    suspend fun createUserSession(userSession: UserSession): AppResult<UserSession>
    suspend fun deleteUserSession(userId: UserId, refreshTokenHash: RefreshTokenHash): AppResult<Unit>
    suspend fun deleteUserSessionById(userSessionId: UserSessionId): AppResult<Unit>
    suspend fun deleteMultipleUserSessions(userId: UserId, sessionIds: List<UserSessionId>): AppResult<Unit>
    suspend fun deleteAllUserSessions(userId: UserId): AppResult<Unit>
    suspend fun deleteAllUserSessionsExceptOne(userId: UserId, userSessionId: UserSessionId): AppResult<Unit>

    suspend fun updateLastAccessed(userSessionId: UserSessionId): AppResult<Unit>

    suspend fun revokeSession(refreshTokenHash: RefreshTokenHash): AppResult<Unit>
    suspend fun revokeAllSessionsForUser(userId: UserId): AppResult<Unit>

    suspend fun getUserSessionById(userSessionId: UserSessionId): AppResult<UserSession?>
    suspend fun getUserSessionByHash(userId: UserId?, refreshTokenHash: RefreshTokenHash): AppResult<UserSession?>
    suspend fun getAllUserSessions(userId: UserId): AppResult<List<UserSession>>
}