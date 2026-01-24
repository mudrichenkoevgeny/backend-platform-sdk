package com.mudrichenkoevgeny.backend.feature.user.manager.session

import com.mudrichenkoevgeny.backend.core.common.network.request.model.ClientInfo
import com.mudrichenkoevgeny.backend.core.common.result.AppResult
import com.mudrichenkoevgeny.backend.core.database.util.dbQuery
import com.mudrichenkoevgeny.backend.feature.user.config.model.UserConfig
import com.mudrichenkoevgeny.backend.feature.user.database.repository.usersession.UserSessionRepository
import com.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import com.mudrichenkoevgeny.backend.feature.user.model.auth.RefreshToken
import com.mudrichenkoevgeny.backend.feature.user.model.auth.SessionToken
import com.mudrichenkoevgeny.backend.core.common.model.UserId
import com.mudrichenkoevgeny.backend.core.common.model.UserIdentifierId
import com.mudrichenkoevgeny.backend.feature.user.model.session.UserSession
import com.mudrichenkoevgeny.backend.core.common.model.UserSessionId
import com.mudrichenkoevgeny.backend.feature.user.enums.UserAuthProvider
import com.mudrichenkoevgeny.backend.feature.user.security.refreshtokenprovider.RefreshTokenProvider
import com.mudrichenkoevgeny.backend.feature.user.security.tokenprovider.TokenProvider
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManagerImpl @Inject constructor(
    private val userConfig: UserConfig,
    private val jwtTokenProvider: TokenProvider,
    private val refreshTokenProvider: RefreshTokenProvider,
    private val userSessionRepository: UserSessionRepository
) : SessionManager {

    override suspend fun createSession(
        userId: UserId,
        userIdentifierId: UserIdentifierId,
        userIdentifierAuthProvider: UserAuthProvider,
        clientInfo: ClientInfo,
        lastReauthenticatedAt: Instant
    ): AppResult<SessionToken> = dbQuery {
        val userSessionId = UserSessionId(UUID.randomUUID())

        val now = Instant.now()
        val accessExpiry = now.plus(userConfig.getAccessTokenValidityHoursDuration())
        val refreshExpiry = now.plus(userConfig.getRefreshTokenValidityDaysDuration())

        val accessTokenResult = jwtTokenProvider.generateAccessToken(
            userId = userId,
            sessionId = userSessionId,
            issuedAt = now,
            expiration = accessExpiry
        )
        val accessToken = when (accessTokenResult) {
            is AppResult.Success -> accessTokenResult.data
            is AppResult.Error -> return@dbQuery accessTokenResult
        }

        val refreshTokenResult = refreshTokenProvider.getRefreshToken()

        val refreshToken = when (refreshTokenResult) {
            is AppResult.Success -> refreshTokenResult.data
            is AppResult.Error -> return@dbQuery refreshTokenResult
        }

        val refreshTokenHashResult = refreshTokenProvider.getRefreshTokenHash(refreshToken)

        val refreshTokenHash = when (refreshTokenHashResult) {
            is AppResult.Success -> refreshTokenHashResult.data
            is AppResult.Error -> return@dbQuery refreshTokenHashResult
        }

        val userSession = UserSession(
            id = userSessionId,
            userId = userId,
            userIdentifierId = userIdentifierId,
            userIdentifierAuthProvider = userIdentifierAuthProvider,
            refreshTokenHash = refreshTokenHash,
            expiresAt = refreshExpiry,
            revoked = false,
            userAgent = clientInfo.userAgent,
            ipAddress = clientInfo.ipAddress,
            userDeviceId = clientInfo.deviceId,
            userDeviceName = clientInfo.deviceName,
            createdAt = now,
            updatedAt = null,
            lastAccessedAt = now,
            lastReauthenticatedAt = lastReauthenticatedAt
        )

        when (val createUserSessionResult = userSessionRepository.createUserSession(userSession)) {
            is AppResult.Success -> AppResult.Success(
                SessionToken(
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    expiresAt = createUserSessionResult.data.expiresAt
                )
            )
            is AppResult.Error -> createUserSessionResult
        }
    }

    override suspend fun refreshSession(
        userId: UserId?,
        refreshToken: RefreshToken,
        clientInfo: ClientInfo
    ): AppResult<SessionToken> = dbQuery {
        val refreshTokenHashResult = refreshTokenProvider.getRefreshTokenHash(refreshToken)

        val refreshTokenHash = when (refreshTokenHashResult) {
            is AppResult.Success -> refreshTokenHashResult.data
            is AppResult.Error -> return@dbQuery refreshTokenHashResult
        }

        val currentUserSessionResult = userSessionRepository.getUserSessionByHash(
            userId = userId,
            refreshTokenHash = refreshTokenHash
        )
        val currentUserSession = when (currentUserSessionResult) {
            is AppResult.Success -> currentUserSessionResult.data
            is AppResult.Error -> return@dbQuery currentUserSessionResult
        }

        if (currentUserSession == null || !currentUserSession.isValid(clientInfo, Instant.now())) {
            return@dbQuery AppResult.Error(UserError.InvalidRefreshToken())
        }

        userSessionRepository.deleteUserSessionById(currentUserSession.id)

        createSession(
            userId = currentUserSession.userId,
            userIdentifierId = currentUserSession.userIdentifierId,
            userIdentifierAuthProvider = currentUserSession.userIdentifierAuthProvider,
            clientInfo = clientInfo,
            lastReauthenticatedAt = currentUserSession.lastReauthenticatedAt
        )
    }

    override suspend fun revokeSessionById(userSessionId: UserSessionId): AppResult<Unit> = dbQuery {
        userSessionRepository.deleteUserSessionById(userSessionId)
    }

    override suspend fun revokeSession(userId: UserId, refreshToken: RefreshToken): AppResult<Unit> = dbQuery {
        val refreshTokenHashResult = refreshTokenProvider.getRefreshTokenHash(refreshToken)

        val refreshTokenHash = when (refreshTokenHashResult) {
            is AppResult.Success -> refreshTokenHashResult.data
            is AppResult.Error -> return@dbQuery refreshTokenHashResult
        }

        userSessionRepository.deleteUserSession(
            userId = userId,
            refreshTokenHash = refreshTokenHash
        )
    }

    override suspend fun revokeMultipleUserSessions(
        userId: UserId,
        sessionIds: List<UserSessionId>
    ): AppResult<Unit> = dbQuery {
        userSessionRepository.deleteMultipleUserSessions(userId, sessionIds)
    }

    override suspend fun revokeAllUserSessions(userId: UserId): AppResult<Unit> = dbQuery {
        userSessionRepository.deleteAllUserSessions(userId)
    }

    override suspend fun getUserSessionById(userSessionId: UserSessionId): AppResult<UserSession?> = dbQuery {
        userSessionRepository.getUserSessionById(userSessionId)
    }

    override suspend fun getAllUserSessions(userId: UserId): AppResult<List<UserSession>> = dbQuery {
        userSessionRepository.getAllUserSessions(userId)
    }

    override suspend fun revokeAllUserSessionsExceptOne(
        userId: UserId,
        userSessionId: UserSessionId
    ): AppResult<Unit> = dbQuery {
        userSessionRepository.deleteAllUserSessionsExceptOne(userId, userSessionId)
    }
}