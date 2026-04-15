package io.github.mudrichenkoevgeny.backend.feature.user.database.repository.usersession

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.pagination.getNumOfTotalPages
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.util.toJavaInstant
import io.github.mudrichenkoevgeny.backend.core.common.util.toKotlinInstant
import io.github.mudrichenkoevgeny.backend.core.database.extensions.applyPagination
import io.github.mudrichenkoevgeny.backend.core.database.extensions.substringSqlLikePattern
import io.github.mudrichenkoevgeny.backend.core.database.mapper.toExposedSortOrder
import io.github.mudrichenkoevgeny.backend.feature.user.database.table.UsersTable
import io.github.mudrichenkoevgeny.backend.feature.user.database.table.UserSessionsTable
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.UserRoleAccessFilter
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientDeviceId
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientDeviceInfo
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientType
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.SortOrder
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserSortValues
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionInternal
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.token.RefreshTokenHash
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Instant as JavaInstant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default [UserSessionRepository] implementation backed by Exposed and [UserSessionsTable].
 *
 * Performs synchronous Exposed DSL operations and maps [ResultRow] values into [UserSessionInternal] models.
 * Returns [CommonError.Database] when inserts report no affected rows.
 */
@Singleton
class UserSessionRepositoryImpl @Inject constructor() : UserSessionRepository {

    override suspend fun createUserSession(
        userSession: UserSessionInternal
    ): AppResult<UserSessionInternal> {
        val inserted = UserSessionsTable.insert { userSessionRow ->
            userSessionRow[id] = userSession.id.value
            userSessionRow[userId] = userSession.userId.value
            userSessionRow[identifierId] = userSession.identifierId.value
            userSessionRow[identifierAuthProvider] = userSession.identifierAuthProvider
            userSessionRow[identifier] = userSession.identifier
            userSessionRow[refreshTokenHash] = userSession.refreshTokenHash.value
            userSessionRow[expiresAt] = userSession.expiresAt?.toJavaInstant()
            userSessionRow[revoked] = userSession.revoked
            userSessionRow[clientType] = userSession.deviceInfo.clientType
            userSessionRow[userAgent] = userSession.userAgent
            userSessionRow[ipAddress] = userSession.ipAddress
            userSessionRow[language] = userSession.deviceInfo.language
            userSessionRow[deviceId] = userSession.deviceInfo.deviceId?.value
            userSessionRow[deviceName] = userSession.deviceInfo.deviceName
            userSessionRow[appVersion] = userSession.deviceInfo.appVersion
            userSessionRow[operationSystemVersion] = userSession.deviceInfo.operationSystemVersion
            userSessionRow[createdAt] = userSession.createdAt.toJavaInstant()
            userSessionRow[updatedAt] = userSession.updatedAt?.toJavaInstant()
            userSessionRow[lastAccessedAt] = userSession.lastAccessedAt?.toJavaInstant()
            userSessionRow[lastReauthenticatedAt] = userSession.lastReauthenticatedAt?.toJavaInstant()
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
            (UserSessionsTable.refreshTokenHash eq refreshTokenHash.value) and
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
                it[UserSessionsTable.lastAccessedAt] = JavaInstant.now()
            }

        return AppResult.Success(Unit)
    }

    override suspend fun revokeSession(
        refreshTokenHash: RefreshTokenHash
    ): AppResult<Unit> {
        UserSessionsTable
            .update({ UserSessionsTable.refreshTokenHash eq refreshTokenHash.value }) {
                it[UserSessionsTable.revoked] = true
                it[UserSessionsTable.updatedAt] = JavaInstant.now()
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
                it[UserSessionsTable.updatedAt] = JavaInstant.now()
            }

        return AppResult.Success(Unit)
    }

    override suspend fun getUserSessionById(
        userSessionId: UserSessionId
    ): AppResult<UserSessionInternal?> {
        val resultRow = UserSessionsTable
            .selectAll()
            .where { UserSessionsTable.id eq userSessionId.value }
            .singleOrNull()

        return AppResult.Success(resultRow?.toUserSessionInternal())
    }

    override suspend fun getUserSessionByHash(
        userId: UserId?,
        refreshTokenHash: RefreshTokenHash
    ): AppResult<UserSessionInternal?> {
        val query = UserSessionsTable.selectAll()

        userId?.let { id ->
            query.andWhere { UserSessionsTable.userId eq id.value }
        }

        query.andWhere { UserSessionsTable.refreshTokenHash eq refreshTokenHash.value }

        val resultRow = query.singleOrNull()

        return AppResult.Success(resultRow?.toUserSessionInternal())
    }

    override suspend fun getAllUserSessions(
        userId: UserId
    ): AppResult<List<UserSessionInternal>> {
        val query = UserSessionsTable
            .selectAll()
            .where { UserSessionsTable.userId eq userId.value }

        val userSessions = query.map { it.toUserSessionInternal() }

        return AppResult.Success(userSessions)
    }

    override suspend fun getUserSessionsByIdentifierId(
        userIdentifierId: UserIdentifierId,
        userId: UserId?
    ): AppResult<List<UserSessionInternal>> {
        var query = UserSessionsTable
            .selectAll()
            .where { UserSessionsTable.identifierId eq userIdentifierId.value }

        userId?.let { query = query.andWhere { UserSessionsTable.userId eq it.value } }

        val userSessions = query.map { it.toUserSessionInternal() }

        return AppResult.Success(userSessions)
    }

    override suspend fun getUserSessionsList(
        accessFilter: UserRoleAccessFilter,
        pageParams: PageParams,
        sortBy: UserSortValues.UserSessionSortBy,
        sortOrder: SortOrder,
        userIds: List<UserId>,
        identifiers: List<String>,
        identifierIds: List<UserIdentifierId>,
        identifierAuthProviders: List<UserAuthProvider>,
        revokedValues: List<Boolean>,
        clientTypes: List<ClientType>,
        userAgents: List<String>,
        ipAddresses: List<String>,
        languages: List<String>,
        deviceIds: List<String>,
        deviceNames: List<String>,
        appVersions: List<String>,
        operationSystemVersions: List<String>
    ): AppResult<PagedResult<UserSessionInternal>> {
        var query = UserSessionsTable.selectAll()

        query = query.andWhere {
            val allowedRoleConditions = accessFilter.allowedUserRoles
                .map { allowedRole -> UsersTable.role eq allowedRole }

            val allowedRolesPredicate = allowedRoleConditions
                .reduceOrNull { acc, condition -> acc or condition }
                ?: Op.FALSE

            val allowedUserIds = UsersTable
                .selectAll()
                .where { allowedRolesPredicate }
                .map { it[UsersTable.id] }

            if (allowedUserIds.isEmpty()) {
                Op.FALSE
            } else {
                UserSessionsTable.userId inList allowedUserIds
            }
        }

        if (userIds.isNotEmpty()) {
            query = query.andWhere { UserSessionsTable.userId inList userIds.map { it.value } }
        }
        addOrLikeFilter(query, UserSessionsTable.identifier, identifiers)?.let { query = it }
        if (identifierIds.isNotEmpty()) {
            query = query.andWhere { UserSessionsTable.identifierId inList identifierIds.map { it.value } }
        }
        if (identifierAuthProviders.isNotEmpty()) {
            query = query.andWhere { UserSessionsTable.identifierAuthProvider inList identifierAuthProviders }
        }
        if (revokedValues.isNotEmpty()) {
            query = query.andWhere { UserSessionsTable.revoked inList revokedValues }
        }
        if (clientTypes.isNotEmpty()) {
            query = query.andWhere { UserSessionsTable.clientType inList clientTypes }
        }
        addOrLikeFilter(query, UserSessionsTable.userAgent, userAgents)?.let { query = it }
        addOrLikeFilter(query, UserSessionsTable.ipAddress, ipAddresses)?.let { query = it }
        addOrLikeFilter(query, UserSessionsTable.language, languages)?.let { query = it }
        addOrLikeFilter(query, UserSessionsTable.deviceId, deviceIds)?.let { query = it }
        addOrLikeFilter(query, UserSessionsTable.deviceName, deviceNames)?.let { query = it }
        addOrLikeFilter(query, UserSessionsTable.appVersion, appVersions)?.let { query = it }
        addOrLikeFilter(query, UserSessionsTable.operationSystemVersion, operationSystemVersions)?.let { query = it }

        val totalCount = query.count()

        val sortColumn = when (sortBy) {
            UserSortValues.UserSessionSortBy.LAST_ACCESSED_AT -> UserSessionsTable.lastAccessedAt
            UserSortValues.UserSessionSortBy.LAST_REAUTHENTICATED_AT -> UserSessionsTable.lastReauthenticatedAt
            UserSortValues.UserSessionSortBy.EXPIRES_AT -> UserSessionsTable.expiresAt
            UserSortValues.UserSessionSortBy.CREATED_AT -> UserSessionsTable.createdAt
            UserSortValues.UserSessionSortBy.UPDATED_AT -> UserSessionsTable.updatedAt
        }
        val exposedSortOrder = sortOrder.toExposedSortOrder()

        val sessions = query
            .orderBy(sortColumn to exposedSortOrder)
            .applyPagination(pageParams)
            .map { it.toUserSessionInternal() }

        val totalPages = getNumOfTotalPages(totalCount, pageParams.size)

        return AppResult.Success(
            PagedResult(
                items = sessions,
                totalCount = totalCount,
                pageNumber = pageParams.page,
                pageSize = pageParams.size,
                totalPages = totalPages
            )
        )
    }

    private fun ResultRow.toUserSessionInternal(): UserSessionInternal = UserSessionInternal(
        id = UserSessionId(this[UserSessionsTable.id].value),
        userId = UserId(this[UserSessionsTable.userId].value),
        identifier = this[UserSessionsTable.identifier],
        identifierId = UserIdentifierId(this[UserSessionsTable.identifierId].value),
        identifierAuthProvider = this[UserSessionsTable.identifierAuthProvider],
        refreshTokenHash = RefreshTokenHash(this[UserSessionsTable.refreshTokenHash]),
        expiresAt = this[UserSessionsTable.expiresAt]?.toKotlinInstant(),
        revoked = this[UserSessionsTable.revoked],
        deviceInfo = ClientDeviceInfo(
            deviceId = this[UserSessionsTable.deviceId]?.let { ClientDeviceId(it) },
            deviceName = this[UserSessionsTable.deviceName],
            clientType = this[UserSessionsTable.clientType],
            language = this[UserSessionsTable.language],
            appVersion = this[UserSessionsTable.appVersion],
            operationSystemVersion = this[UserSessionsTable.operationSystemVersion]
        ),
        userAgent = this[UserSessionsTable.userAgent],
        ipAddress = this[UserSessionsTable.ipAddress],
        lastAccessedAt = this[UserSessionsTable.lastAccessedAt]?.toKotlinInstant(),
        lastReauthenticatedAt = this[UserSessionsTable.lastReauthenticatedAt]?.toKotlinInstant(),
        isSensitiveValuesMasked = false,
        createdAt = this[UserSessionsTable.createdAt].toKotlinInstant(),
        updatedAt = this[UserSessionsTable.updatedAt]?.toKotlinInstant()
    )

    private fun addOrLikeFilter(
        query: Query,
        column: Column<String?>,
        rawValues: List<String>
    ): Query? {
        val nonBlank = rawValues.filter { it.isNotBlank() }
        if (nonBlank.isEmpty()) return null

        val predicate = nonBlank
            .map { value -> column.lowerCase() like substringSqlLikePattern(value.lowercase()) }
            .reduce { acc, condition -> acc or condition }

        return query.andWhere { predicate }
    }
}