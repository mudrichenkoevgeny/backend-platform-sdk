package com.mudrichenkoevgeny.backend.feature.user.database.repository.usersession

import com.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import com.mudrichenkoevgeny.backend.core.common.model.UserDeviceId
import com.mudrichenkoevgeny.backend.core.common.result.AppResult
import com.mudrichenkoevgeny.backend.feature.user.database.table.UserSessionsTable
import com.mudrichenkoevgeny.backend.feature.user.model.auth.RefreshTokenHash
import com.mudrichenkoevgeny.backend.core.common.model.UserId
import com.mudrichenkoevgeny.backend.core.common.model.UserIdentifierId
import com.mudrichenkoevgeny.backend.feature.user.model.session.UserSession
import com.mudrichenkoevgeny.backend.core.common.model.UserSessionId
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserSessionRepositoryImpl @Inject constructor() : UserSessionRepository {

    override suspend fun createUserSession(
        userSession: UserSession
    ): AppResult<UserSession> {
        val inserted = UserSessionsTable.insert { userSessionRow ->
            userSessionRow[id] = userSession.id.value
            userSessionRow[userId] = userSession.userId.value
            userSessionRow[userIdentifierId] = userSession.userIdentifierId.value
            userSessionRow[userIdentifierAuthProvider] = userSession.userIdentifierAuthProvider
            userSessionRow[tokenHash] = userSession.refreshTokenHash.value
            userSessionRow[expiresAt] = userSession.expiresAt
            userSessionRow[revoked] = userSession.revoked
            userSessionRow[userAgent] = userSession.userAgent
            userSessionRow[ipAddress] = userSession.ipAddress
            userSessionRow[deviceId] = userSession.userDeviceId?.value
            userSessionRow[deviceName] = userSession.userDeviceName
            userSessionRow[createdAt] = userSession.createdAt
            userSessionRow[updatedAt] = userSession.updatedAt
            userSessionRow[lastAccessedAt] = userSession.lastAccessedAt
            userSessionRow[lastReauthenticatedAt] = userSession.lastReauthenticatedAt
        }

        if (inserted.insertedCount == 0) {
            return AppResult.Error(
                CommonError.Database(
                    "UserSession creation failed for userId=${userSession.userId.value}"
                )
            )
        }

        return AppResult.Success(userSession)
    }

    override suspend fun deleteUserSession(
        userId: UserId,
        refreshTokenHash: RefreshTokenHash
    ): AppResult<Unit> {
        UserSessionsTable.deleteWhere {
            (UserSessionsTable.tokenHash eq refreshTokenHash.value) and
                    (UserSessionsTable.userId eq userId.value)
        }

        return AppResult.Success(Unit)
    }

    override suspend fun deleteUserSessionById(userSessionId: UserSessionId): AppResult<Unit> {
        UserSessionsTable.deleteWhere { UserSessionsTable.id eq userSessionId.value }

        return AppResult.Success(Unit)
    }

    override suspend fun deleteMultipleUserSessions(
        userId: UserId,
        sessionIds: List<UserSessionId>
    ): AppResult<Unit> {
        UserSessionsTable.deleteWhere {
            (UserSessionsTable.userId eq userId.value) and
                    (UserSessionsTable.id inList sessionIds.map { it.value })
        }

        return AppResult.Success(Unit)
    }

    override suspend fun deleteAllUserSessions(
        userId: UserId
    ): AppResult<Unit> {
        UserSessionsTable
            .deleteWhere { UserSessionsTable.userId eq userId.value }

        return AppResult.Success(Unit)
    }

    override suspend fun deleteAllUserSessionsExceptOne(
        userId: UserId,
        userSessionId: UserSessionId
    ): AppResult<Unit> {
        UserSessionsTable.deleteWhere {
            (UserSessionsTable.userId eq userId.value) and
                    (UserSessionsTable.id neq userSessionId.value)
        }

        return AppResult.Success(Unit)
    }

    override suspend fun updateLastAccessed(
        userSessionId: UserSessionId
    ): AppResult<Unit> {
        UserSessionsTable
            .update( { UserSessionsTable.id eq userSessionId.value }) {
                it[UserSessionsTable.lastAccessedAt] = Instant.now()
            }

        return AppResult.Success(Unit)
    }

    override suspend fun revokeSession(
        refreshTokenHash: RefreshTokenHash
    ): AppResult<Unit> {
        UserSessionsTable
            .update({ UserSessionsTable.tokenHash eq refreshTokenHash.value }) {
                it[UserSessionsTable.revoked] = true
                it[UserSessionsTable.updatedAt] = Instant.now()
            }

        return AppResult.Success(Unit)
    }

    override suspend fun revokeAllSessionsForUser(
        userId: UserId
    ): AppResult<Unit> {
        UserSessionsTable
            .update({
                (UserSessionsTable.userId eq userId.value) and
                        (UserSessionsTable.revoked eq false)
            }) {
                it[UserSessionsTable.revoked] = true
                it[UserSessionsTable.updatedAt] = Instant.now()
            }

        return AppResult.Success(Unit)
    }

    override suspend fun getUserSessionById(
        userSessionId: UserSessionId
    ): AppResult<UserSession?> {
        val resultRow = UserSessionsTable
            .selectAll()
            .where { UserSessionsTable.id eq userSessionId.value }
            .singleOrNull()

        return AppResult.Success(resultRow?.toUserSession())
    }

    override suspend fun getUserSessionByHash(
        userId: UserId?,
        refreshTokenHash: RefreshTokenHash
    ): AppResult<UserSession?> {
        val query = UserSessionsTable.selectAll()

        userId?.let { id ->
            query.andWhere { UserSessionsTable.userId eq id.value }
        }

        query.andWhere { UserSessionsTable.tokenHash eq refreshTokenHash.value }

        val resultRow = query.singleOrNull()

        return AppResult.Success(resultRow?.toUserSession())
    }

    override suspend fun getAllUserSessions(
        userId: UserId
    ): AppResult<List<UserSession>> {
        val query = UserSessionsTable
            .selectAll()
            .where { UserSessionsTable.userId eq userId.value }

        val userSessions = query.map { it.toUserSession() }

        return AppResult.Success(userSessions)
    }

    private fun ResultRow.toUserSession(): UserSession = UserSession(
        id = UserSessionId(this[UserSessionsTable.id].value),
        userId = UserId(this[UserSessionsTable.userId].value),
        userIdentifierId = UserIdentifierId(this[UserSessionsTable.userIdentifierId].value),
        userIdentifierAuthProvider = this[UserSessionsTable.userIdentifierAuthProvider],
        refreshTokenHash = RefreshTokenHash(this[UserSessionsTable.tokenHash]),
        expiresAt = this[UserSessionsTable.expiresAt],
        revoked = this[UserSessionsTable.revoked],
        userAgent = this[UserSessionsTable.userAgent],
        ipAddress = this[UserSessionsTable.ipAddress],
        userDeviceId = this[UserSessionsTable.deviceId]?.let { deviceId -> UserDeviceId(deviceId) },
        userDeviceName = this[UserSessionsTable.deviceName],
        createdAt = this[UserSessionsTable.createdAt],
        updatedAt = this[UserSessionsTable.updatedAt],
        lastAccessedAt = this[UserSessionsTable.lastAccessedAt],
        lastReauthenticatedAt = this[UserSessionsTable.lastReauthenticatedAt]
    )
}