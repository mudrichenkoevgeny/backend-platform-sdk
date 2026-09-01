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
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientDeviceInfo
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientType
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.toClientDeviceIdOrNull
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
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.innerJoin
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.select
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
            userSessionRow[userRole] = userSession.userRole
            userSessionRow[identifierId] = userSession.identifierId.value
            userSessionRow[identifierAuthProvider] = userSession.identifierAuthProvider
            userSessionRow[identifier] = userSession.identifier
            userSessionRow[refreshTokenHash] = userSession.refreshTokenHash.value

            userSessionRow[clientType] = userSession.deviceInfo.clientType
            userSessionRow[deviceId] = userSession.deviceInfo.deviceId?.asHexDashString()
            userSessionRow[deviceName] = userSession.deviceInfo.deviceName
            userSessionRow[appVersion] = userSession.deviceInfo.appVersion
            userSessionRow[operationSystemVersion] = userSession.deviceInfo.operationSystemVersion
            userSessionRow[language] = userSession.deviceInfo.language

            userSessionRow[userAgent] = userSession.userAgent
            userSessionRow[ipAddress] = userSession.ipAddress

            userSessionRow[expiresAt] = userSession.expiresAt.toJavaInstant()
            userSessionRow[lastAccessedAt] = userSession.lastAccessedAt.toJavaInstant()
            userSessionRow[lastReauthenticatedAt] = userSession.lastReauthenticatedAt.toJavaInstant()

            userSessionRow[createdAt] = userSession.createdAt.toJavaInstant()
            userSessionRow[updatedAt] = userSession.updatedAt?.toJavaInstant()
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

    override suspend fun deleteUserSessionById(userSessionId: UserSessionId): AppResult<Unit> {
        UserSessionsTable.deleteWhere { UserSessionsTable.id eq userSessionId.value }

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
    ): AppResult<List<UserSessionId>> {
        val deletedIds = UserSessionsTable
            .select(UserSessionsTable.id)
            .where {
                (UserSessionsTable.userId eq userId.value) and
                        (UserSessionsTable.id neq userSessionId.value)
            }
            .map { UserSessionId(it[UserSessionsTable.id].value) }

        UserSessionsTable.deleteWhere {
            (UserSessionsTable.userId eq userId.value) and
                    (UserSessionsTable.id neq userSessionId.value)
        }

        return AppResult.Success(deletedIds)
    }

    override suspend fun deleteLeastRecentlyUsedUserSession(userId: UserId): AppResult<UserSessionId> {
        val oldestSessionId = UserSessionsTable
            .select(UserSessionsTable.id)
            .where { UserSessionsTable.userId eq userId.value }
            .orderBy(UserSessionsTable.lastAccessedAt to org.jetbrains.exposed.v1.core.SortOrder.ASC)
            .limit(1)
            .map { UserSessionId(it[UserSessionsTable.id].value) }
            .firstOrNull()
            ?: return AppResult.Error(
                CommonError.Database("No sessions found for userId=${userId.value}")
            )

        UserSessionsTable.deleteWhere { UserSessionsTable.id eq oldestSessionId.value }

        return AppResult.Success(oldestSessionId)
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

    override suspend fun updateLastReauthenticated(
        userSessionId: UserSessionId
    ): AppResult<Unit> {
        UserSessionsTable
            .update( { UserSessionsTable.id eq userSessionId.value }) {
                it[UserSessionsTable.lastReauthenticatedAt] = JavaInstant.now()
            }

        return AppResult.Success(Unit)
    }

    override suspend fun getUserSessionInternalById(
        userSessionId: UserSessionId
    ): AppResult<UserSessionInternal?> {
        return AppResult.Success(getUserSessionResultRow(userSessionId)?.toUserSessionInternal())
    }

    override suspend fun getUserSessionById(
        userSessionId: UserSessionId
    ): AppResult<UserSession?> {
        return AppResult.Success(getUserSessionResultRow(userSessionId)?.toUserSession())
    }

    private fun getUserSessionResultRow(
        userSessionId: UserSessionId
    ): ResultRow? {
        return UserSessionsTable
            .selectAll()
            .where { UserSessionsTable.id eq userSessionId.value }
            .singleOrNull()
    }

    override suspend fun getUserSessionByHash(
        refreshTokenHash: RefreshTokenHash
    ): AppResult<UserSessionInternal?> {
        val resultRow = UserSessionsTable
            .selectAll()
            .where { UserSessionsTable.refreshTokenHash eq refreshTokenHash.value }
            .singleOrNull()

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

    override suspend fun getUserSessionsPageWithAccessFilter(
        accessFilter: UserRoleAccessFilter,
        pageParams: PageParams,
        sortBy: UserSortValues.UserSessionSortBy,
        sortOrder: SortOrder,
        userIds: List<UserId>,
        userRoles: List<UserRole>,
        identifiers: List<String>,
        identifierIds: List<UserIdentifierId>,
        identifierAuthProviders: List<UserAuthProvider>,
        clientTypes: List<ClientType>,
        userAgents: List<String>,
        ipAddresses: List<String>,
        languages: List<String>,
        deviceIds: List<String>,
        deviceNames: List<String>,
        appVersions: List<String>,
        operationSystemVersions: List<String>
    ): AppResult<PagedResult<UserSession>> {
        val query = UserSessionsTable
            .innerJoin(UsersTable, { userId }, { id })
            .selectAll()

        query.andWhere {
            val allowedRoleConditions = accessFilter.allowedUserRoles
                .map { allowedRole -> UsersTable.role eq allowedRole }

            allowedRoleConditions.reduceOrNull { acc, condition -> acc or condition }
                ?: Op.FALSE
        }

        if (userIds.isNotEmpty()) {
            query.andWhere { UserSessionsTable.userId inList userIds.map { it.value } }
        }

        return executeUserSessionsPagedQuery(
            query = query,
            pageParams = pageParams,
            sortBy = sortBy,
            sortOrder = sortOrder,
            userRoles = userRoles,
            identifiers = identifiers,
            identifierIds = identifierIds,
            identifierAuthProviders = identifierAuthProviders,
            clientTypes = clientTypes,
            userAgents = userAgents,
            ipAddresses = ipAddresses,
            languages = languages,
            deviceIds = deviceIds,
            deviceNames = deviceNames,
            appVersions = appVersions,
            operationSystemVersions = operationSystemVersions
        )
    }

    override suspend fun getUserSessionsPageByUserId(
        userId: UserId,
        pageParams: PageParams,
        sortBy: UserSortValues.UserSessionSortBy,
        sortOrder: SortOrder,
        identifiers: List<String>,
        identifierIds: List<UserIdentifierId>,
        identifierAuthProviders: List<UserAuthProvider>,
        clientTypes: List<ClientType>,
        userAgents: List<String>,
        ipAddresses: List<String>,
        languages: List<String>,
        deviceIds: List<String>,
        deviceNames: List<String>,
        appVersions: List<String>,
        operationSystemVersions: List<String>
    ): AppResult<PagedResult<UserSession>> {
        val query = UserSessionsTable
            .selectAll()
            .where { UserSessionsTable.userId eq userId.value }

        return executeUserSessionsPagedQuery(
            query = query,
            pageParams = pageParams,
            sortBy = sortBy,
            sortOrder = sortOrder,
            userRoles = emptyList(),
            identifiers = identifiers,
            identifierIds = identifierIds,
            identifierAuthProviders = identifierAuthProviders,
            clientTypes = clientTypes,
            userAgents = userAgents,
            ipAddresses = ipAddresses,
            languages = languages,
            deviceIds = deviceIds,
            deviceNames = deviceNames,
            appVersions = appVersions,
            operationSystemVersions = operationSystemVersions
        )
    }

    private fun executeUserSessionsPagedQuery(
        query: Query,
        pageParams: PageParams,
        sortBy: UserSortValues.UserSessionSortBy,
        sortOrder: SortOrder,
        userRoles: List<UserRole>,
        identifiers: List<String>,
        identifierIds: List<UserIdentifierId>,
        identifierAuthProviders: List<UserAuthProvider>,
        clientTypes: List<ClientType>,
        userAgents: List<String>,
        ipAddresses: List<String>,
        languages: List<String>,
        deviceIds: List<String>,
        deviceNames: List<String>,
        appVersions: List<String>,
        operationSystemVersions: List<String>
    ): AppResult<PagedResult<UserSession>> {
        if (userRoles.isNotEmpty()) {
            if (query.targets.none { it == UsersTable }) {
                query.adjustColumnSet { innerJoin(UsersTable, { UserSessionsTable.userId }, { id }) }
            }
            query.andWhere { UsersTable.role inList userRoles }
        }

        addOrLikeFilter(query, UserSessionsTable.identifier, identifiers)

        if (identifierIds.isNotEmpty()) {
            query.andWhere { UserSessionsTable.identifierId inList identifierIds.map { it.value } }
        }
        if (identifierAuthProviders.isNotEmpty()) {
            query.andWhere { UserSessionsTable.identifierAuthProvider inList identifierAuthProviders }
        }
        if (clientTypes.isNotEmpty()) {
            query.andWhere { UserSessionsTable.clientType inList clientTypes }
        }

        addOrLikeFilter(query, UserSessionsTable.userAgent, userAgents)
        addOrLikeFilter(query, UserSessionsTable.ipAddress, ipAddresses)
        addOrLikeFilter(query, UserSessionsTable.language, languages)
        addOrLikeFilter(query, UserSessionsTable.deviceId, deviceIds)
        addOrLikeFilter(query, UserSessionsTable.deviceName, deviceNames)
        addOrLikeFilter(query, UserSessionsTable.appVersion, appVersions)
        addOrLikeFilter(query, UserSessionsTable.operationSystemVersion, operationSystemVersions)

        val totalCount = query.count()
        val totalPages = getNumOfTotalPages(totalCount, pageParams.size)

        val sortColumn = when (sortBy) {
            UserSortValues.UserSessionSortBy.LAST_ACCESSED_AT -> UserSessionsTable.lastAccessedAt
            UserSortValues.UserSessionSortBy.LAST_REAUTHENTICATED_AT -> UserSessionsTable.lastReauthenticatedAt
            UserSortValues.UserSessionSortBy.EXPIRES_AT -> UserSessionsTable.expiresAt
            UserSortValues.UserSessionSortBy.CREATED_AT -> UserSessionsTable.createdAt
            UserSortValues.UserSessionSortBy.UPDATED_AT -> UserSessionsTable.updatedAt
        }

        val items = query
            .orderBy(sortColumn to sortOrder.toExposedSortOrder())
            .applyPagination(pageParams)
            .map { it.toUserSession() }

        return AppResult.Success(
            PagedResult(
                items = items,
                totalCount = totalCount,
                pageNumber = pageParams.page,
                pageSize = pageParams.size,
                totalPages = totalPages
            )
        )
    }

    private fun <T : String?> addOrLikeFilter(
        query: Query,
        column: Column<T>,
        rawValues: List<String>
    ) {
        val nonBlank = rawValues.filter { it.isNotBlank() }
        if (nonBlank.isEmpty()) return

        val predicate = nonBlank
            .map { value ->
                (column.lowerCase() like substringSqlLikePattern(value.lowercase())) as Op<Boolean>
            }
            .reduce { acc, condition -> acc or condition }

        query.andWhere { predicate }
    }

    private fun ResultRow.toUserSessionInternal(): UserSessionInternal = UserSessionInternal(
        id = UserSessionId(this[UserSessionsTable.id].value),
        userId = UserId(this[UserSessionsTable.userId].value),
        userRole = this[UserSessionsTable.userRole],
        identifier = this[UserSessionsTable.identifier],
        identifierId = UserIdentifierId(this[UserSessionsTable.identifierId].value),
        identifierAuthProvider = this[UserSessionsTable.identifierAuthProvider],
        refreshTokenHash = RefreshTokenHash(this[UserSessionsTable.refreshTokenHash]),
        deviceInfo = ClientDeviceInfo(
            deviceId = this[UserSessionsTable.deviceId]?.toClientDeviceIdOrNull(),
            deviceName = this[UserSessionsTable.deviceName],
            clientType = this[UserSessionsTable.clientType],
            language = this[UserSessionsTable.language],
            appVersion = this[UserSessionsTable.appVersion],
            operationSystemVersion = this[UserSessionsTable.operationSystemVersion]
        ),
        userAgent = this[UserSessionsTable.userAgent],
        ipAddress = this[UserSessionsTable.ipAddress],
        expiresAt = this[UserSessionsTable.expiresAt].toKotlinInstant(),
        lastAccessedAt = this[UserSessionsTable.lastAccessedAt].toKotlinInstant(),
        lastReauthenticatedAt = this[UserSessionsTable.lastReauthenticatedAt].toKotlinInstant(),
        createdAt = this[UserSessionsTable.createdAt].toKotlinInstant(),
        updatedAt = this[UserSessionsTable.updatedAt]?.toKotlinInstant()
    )

    private fun ResultRow.toUserSession(): UserSession = UserSession(
        id = UserSessionId(this[UserSessionsTable.id].value),
        userId = UserId(this[UserSessionsTable.userId].value),
        userRole = this[UserSessionsTable.userRole],
        identifier = this[UserSessionsTable.identifier],
        identifierId = UserIdentifierId(this[UserSessionsTable.identifierId].value),
        identifierAuthProvider = this[UserSessionsTable.identifierAuthProvider],
        deviceInfo = ClientDeviceInfo(
            deviceId = this[UserSessionsTable.deviceId]?.toClientDeviceIdOrNull(),
            deviceName = this[UserSessionsTable.deviceName],
            clientType = this[UserSessionsTable.clientType],
            language = this[UserSessionsTable.language],
            appVersion = this[UserSessionsTable.appVersion],
            operationSystemVersion = this[UserSessionsTable.operationSystemVersion]
        ),
        userAgent = this[UserSessionsTable.userAgent],
        ipAddress = this[UserSessionsTable.ipAddress],
        expiresAt = this[UserSessionsTable.expiresAt].toKotlinInstant(),
        lastAccessedAt = this[UserSessionsTable.lastAccessedAt].toKotlinInstant(),
        lastReauthenticatedAt = this[UserSessionsTable.lastReauthenticatedAt].toKotlinInstant(),
        isSensitiveValuesMasked = false,
        createdAt = this[UserSessionsTable.createdAt].toKotlinInstant(),
        updatedAt = this[UserSessionsTable.updatedAt]?.toKotlinInstant()
    )
}