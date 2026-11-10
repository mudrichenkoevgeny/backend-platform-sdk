package io.github.mudrichenkoevgeny.backend.feature.user.database.repository.usersession

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.pagination.getNumOfTotalPages
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.util.toJavaInstant
import io.github.mudrichenkoevgeny.backend.core.common.util.toKotlinInstant
import io.github.mudrichenkoevgeny.backend.core.database.dsl.applyPagination
import io.github.mudrichenkoevgeny.backend.core.database.dsl.applyTimestampFilter
import io.github.mudrichenkoevgeny.backend.core.database.dsl.substringSqlLikePattern
import io.github.mudrichenkoevgeny.backend.core.database.mapper.toExposedSortOrder
import io.github.mudrichenkoevgeny.backend.feature.user.database.table.UserIdentifiersTable
import io.github.mudrichenkoevgeny.backend.feature.user.database.table.UserSessionsTable
import io.github.mudrichenkoevgeny.backend.feature.user.database.table.UsersTable
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.UserRoleAccessFilter
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.network.model.ManagementSessionListQueryParams
import io.github.mudrichenkoevgeny.backend.feature.user.network.model.SelfSessionListQueryParams
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientDeviceId
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientDeviceInfo
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientType
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.SortOrder
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserSessionSortBy
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionInternal
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.token.RefreshTokenHash
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.core.notInList
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.uuid.Uuid
import org.jetbrains.exposed.v1.core.SortOrder as ExposedSortOrder
import java.time.Instant as JavaInstant
import kotlin.time.Instant as KotlinInstant

/**
 * Default [UserSessionRepository] implementation backed by Exposed and [UserSessionsTable].
 */
@Singleton
public class UserSessionRepositoryImpl @Inject constructor() : UserSessionRepository {
    override suspend fun createUserSession(userSession: UserSessionInternal): AppResult<UserSessionInternal> {
        return try {
            val inserted = UserSessionsTable.insert { userSessionRow ->
                userSessionRow[id] = userSession.id.value
                userSessionRow[userId] = userSession.userId.value
                userSessionRow[userRole] = userSession.userRole
                userSessionRow[identifierId] = userSession.identifierId.value
                userSessionRow[identifierAuthProvider] = userSession.identifierAuthProvider
                userSessionRow[identifier] = userSession.identifier
                userSessionRow[identifierDisplayName] = userSession.identifierDisplayName
                userSessionRow[refreshTokenHash] = userSession.refreshTokenHash.value

                userSessionRow[clientType] = userSession.clientDeviceInfo.clientType
                userSessionRow[deviceId] = userSession.clientDeviceInfo.deviceId?.value
                userSessionRow[deviceName] = userSession.clientDeviceInfo.deviceName
                userSessionRow[appVersion] = userSession.clientDeviceInfo.appVersion
                userSessionRow[operationSystemVersion] = userSession.clientDeviceInfo.operationSystemVersion
                userSessionRow[language] = userSession.clientDeviceInfo.language

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

            AppResult.Success(userSession)
        } catch (e: ExposedSQLException) {
            AppResult.Error(CommonError.Database("Error creating user session: ${e.message}"))
        }
    }

    override suspend fun deleteUserSessionById(userSessionId: UserSessionId): AppResult<Unit> = try {
        UserSessionsTable.deleteWhere { UserSessionsTable.id eq userSessionId.value }
        AppResult.Success(Unit)
    } catch (e: ExposedSQLException) {
        AppResult.Error(CommonError.Database("Error deleting user session: ${e.message}"))
    }

    override suspend fun deleteAllUserSessions(userId: UserId): AppResult<Unit> = try {
        UserSessionsTable.deleteWhere { UserSessionsTable.userId eq userId.value }
        AppResult.Success(Unit)
    } catch (e: ExposedSQLException) {
        AppResult.Error(CommonError.Database("Error deleting all user sessions: ${e.message}"))
    }

    override suspend fun deleteAllUserSessionsExceptOne(
        userId: UserId,
        userSessionId: UserSessionId
    ): AppResult<List<UserSessionId>> = try {
        val deletedIds = UserSessionsTable.selectAll().where {
            (UserSessionsTable.userId eq userId.value) and (UserSessionsTable.id neq userSessionId.value)
        }.map { UserSessionId(it[UserSessionsTable.id].value) }

        UserSessionsTable.deleteWhere {
            (UserSessionsTable.userId eq userId.value) and (UserSessionsTable.id neq userSessionId.value)
        }

        AppResult.Success(deletedIds)
    } catch (e: ExposedSQLException) {
        AppResult.Error(CommonError.Database("Error deleting user sessions except one: ${e.message}"))
    }

    override suspend fun deleteLeastRecentlyUsedUserSession(userId: UserId): AppResult<UserSessionId> {
        return try {
            val oldestSessionId = UserSessionsTable.selectAll().where { UserSessionsTable.userId eq userId.value }
                .orderBy(UserSessionsTable.lastAccessedAt to ExposedSortOrder.ASC).limit(1)
                .map { UserSessionId(it[UserSessionsTable.id].value) }.firstOrNull() ?: return AppResult.Error(
                CommonError.Database("No sessions found for userId=${userId.value}")
            )

            UserSessionsTable.deleteWhere { UserSessionsTable.id eq oldestSessionId.value }

            AppResult.Success(oldestSessionId)
        } catch (e: ExposedSQLException) {
            AppResult.Error(CommonError.Database("Error deleting least recently used session: ${e.message}"))
        }
    }

    override suspend fun updateLastAccessed(userSessionId: UserSessionId): AppResult<Unit> {
        return try {
            val updatedCount = UserSessionsTable.update({ UserSessionsTable.id eq userSessionId.value }) {
                it[UserSessionsTable.lastAccessedAt] = JavaInstant.now()
            }

            if (updatedCount == 0) {
                return AppResult.Error(UserError.InvalidSession())
            }

            AppResult.Success(Unit)
        } catch (e: ExposedSQLException) {
            AppResult.Error(CommonError.Database("Error updating session last accessed: ${e.message}"))
        }
    }

    override suspend fun updateLastReauthenticated(userSessionId: UserSessionId): AppResult<Unit> {
        return try {
            val updatedCount = UserSessionsTable.update({ UserSessionsTable.id eq userSessionId.value }) {
                it[UserSessionsTable.lastReauthenticatedAt] = JavaInstant.now()
            }

            if (updatedCount == 0) {
                return AppResult.Error(UserError.InvalidSession())
            }

            AppResult.Success(Unit)
        } catch (e: ExposedSQLException) {
            AppResult.Error(CommonError.Database("Error updating session last reauthenticated: ${e.message}"))
        }
    }

    override suspend fun getUserSessionInternalById(userSessionId: UserSessionId): AppResult<UserSessionInternal?> =
        try {
            AppResult.Success(getUserSessionResultRow(userSessionId)?.toUserSessionInternal())
        } catch (e: ExposedSQLException) {
            AppResult.Error(CommonError.Database("Error retrieving user session by id: ${e.message}"))
        }

    override suspend fun getUserSessionByHash(refreshTokenHash: RefreshTokenHash): AppResult<UserSessionInternal?> =
        try {
            val row = UserSessionsTable.selectAll()
                .where { UserSessionsTable.refreshTokenHash eq refreshTokenHash.value }.singleOrNull()

            AppResult.Success(row?.toUserSessionInternal())
        } catch (e: ExposedSQLException) {
            AppResult.Error(CommonError.Database("Error retrieving user session by hash: ${e.message}"))
        }

    override suspend fun getAllUserSessions(userId: UserId): AppResult<List<UserSessionInternal>> = try {
        val rows = UserSessionsTable.selectAll().where { UserSessionsTable.userId eq userId.value }
            .map { it.toUserSessionInternal() }

        AppResult.Success(rows)
    } catch (e: ExposedSQLException) {
        AppResult.Error(CommonError.Database("Error retrieving all user sessions: ${e.message}"))
    }

    override suspend fun getUserSessionsByIdentifierId(
        userIdentifierId: UserIdentifierId,
        userId: UserId?
    ): AppResult<List<UserSessionInternal>> = try {
        var query = UserSessionsTable.selectAll().where { UserSessionsTable.identifierId eq userIdentifierId.value }

        if (userId != null) {
            query = query.andWhere { UserSessionsTable.userId eq userId.value }
        }

        val rows = query.map { it.toUserSessionInternal() }
        AppResult.Success(rows)
    } catch (e: ExposedSQLException) {
        AppResult.Error(CommonError.Database("Error retrieving user sessions by identifier id: ${e.message}"))
    }

    private fun getUserSessionResultRow(userSessionId: UserSessionId): ResultRow? =
        UserSessionsTable.selectAll().where { UserSessionsTable.id eq userSessionId.value }.limit(1).singleOrNull()

    override suspend fun getUserSessionsPageWithAccessFilter(
        accessFilter: UserRoleAccessFilter,
        queryParams: ManagementSessionListQueryParams
    ): AppResult<PagedResult<UserSessionInternal>> = try {
        var query = UserSessionsTable.innerJoin(UsersTable).selectAll()

        query.andWhere {
            val allowedRoleConditions =
                accessFilter.allowedUserRoles.map { allowedRole -> UsersTable.role eq allowedRole }

            allowedRoleConditions.reduceOrNull { acc, condition -> acc or condition } ?: Op.FALSE
        }

        if (queryParams.userIds.isNotEmpty()) {
            val userEntityIds = queryParams.userIds.mapNotNull { idStr ->
                runCatching {
                    EntityID(
                        Uuid.parse(idStr),
                        UsersTable
                    )
                }.getOrNull()
            }
            if (userEntityIds.isNotEmpty()) {
                query = query.andWhere { UserSessionsTable.userId inList userEntityIds }
            }
        }

        if (queryParams.userRoles.isNotEmpty()) {
            query = query.andWhere { UsersTable.role inList queryParams.userRoles }
        }
        if (queryParams.excludeUserRoles.isNotEmpty()) {
            query = query.andWhere { UsersTable.role notInList queryParams.excludeUserRoles }
        }

        query = query.applyCommonFilters(
            sessionIds = queryParams.sessionIds,
            identifier = queryParams.identifier,
            identifierIds = queryParams.identifierIds,
            identifierDisplayName = queryParams.identifierDisplayName,
            identifierAuthProviders = queryParams.identifierAuthProviders,
            excludeIdentifierAuthProviders = queryParams.excludeIdentifierAuthProviders,
            clientTypes = queryParams.clientTypes,
            excludeClientTypes = queryParams.excludeClientTypes,
            isClientTypeSet = queryParams.isClientTypeSet,
            deviceId = queryParams.deviceId,
            isDeviceIdSet = queryParams.isDeviceIdSet,
            deviceName = queryParams.deviceName,
            isDeviceNameSet = queryParams.isDeviceNameSet,
            language = queryParams.language,
            isLanguageSet = queryParams.isLanguageSet,
            appVersion = queryParams.appVersion,
            isAppVersionSet = queryParams.isAppVersionSet,
            osVersion = queryParams.osVersion,
            isOperationSystemVersionSet = queryParams.isOperationSystemVersionSet,
            userAgent = queryParams.userAgent,
            isUserAgentSet = queryParams.isUserAgentSet,
            ipAddress = queryParams.ipAddress,
            isIpAddressSet = queryParams.isIpAddressSet,
            expiresAtFrom = queryParams.expiresAtFrom,
            expiresAtTo = queryParams.expiresAtTo,
            lastAccessedAtFrom = queryParams.lastAccessedAtFrom,
            lastAccessedAtTo = queryParams.lastAccessedAtTo,
            lastReauthenticatedAtFrom = queryParams.lastReauthenticatedAtFrom,
            lastReauthenticatedAtTo = queryParams.lastReauthenticatedAtTo,
            createdAtFrom = queryParams.createdAtFrom,
            createdAtTo = queryParams.createdAtTo,
            updatedAtFrom = queryParams.updatedAtFrom,
            updatedAtTo = queryParams.updatedAtTo,
            isUpdatedAtSet = queryParams.isUpdatedAtSet,
            searchQuery = queryParams.searchQuery
        )

        executeUserSessionsPagedQuery(
            query = query,
            pageNumber = queryParams.listing.pageParams.page,
            pageSize = queryParams.listing.pageParams.size,
            sortBy = queryParams.listing.sortBy,
            sortOrder = queryParams.listing.sortOrder
        )
    } catch (e: ExposedSQLException) {
        AppResult.Error(CommonError.Database("Error executing user sessions paged query: ${e.message}"))
    }

    override suspend fun getUserSessionsPageByUserId(
        userId: UserId,
        queryParams: SelfSessionListQueryParams
    ): AppResult<PagedResult<UserSessionInternal>> = try {
        var query = UserSessionsTable.selectAll().where { UserSessionsTable.userId eq userId.value }

        query = query.applyCommonFilters(
            sessionIds = queryParams.sessionIds,
            identifier = queryParams.identifier,
            identifierIds = queryParams.identifierIds,
            identifierDisplayName = queryParams.identifierDisplayName,
            identifierAuthProviders = queryParams.identifierAuthProviders,
            excludeIdentifierAuthProviders = queryParams.excludeIdentifierAuthProviders,
            clientTypes = queryParams.clientTypes,
            excludeClientTypes = queryParams.excludeClientTypes,
            isClientTypeSet = queryParams.isClientTypeSet,
            deviceId = queryParams.deviceId,
            isDeviceIdSet = queryParams.isDeviceIdSet,
            deviceName = queryParams.deviceName,
            isDeviceNameSet = queryParams.isDeviceNameSet,
            language = queryParams.language,
            isLanguageSet = queryParams.isLanguageSet,
            appVersion = queryParams.appVersion,
            isAppVersionSet = queryParams.isAppVersionSet,
            osVersion = queryParams.osVersion,
            isOperationSystemVersionSet = queryParams.isOperationSystemVersionSet,
            userAgent = queryParams.userAgent,
            isUserAgentSet = queryParams.isUserAgentSet,
            ipAddress = queryParams.ipAddress,
            isIpAddressSet = queryParams.isIpAddressSet,
            expiresAtFrom = queryParams.expiresAtFrom,
            expiresAtTo = queryParams.expiresAtTo,
            lastAccessedAtFrom = queryParams.lastAccessedAtFrom,
            lastAccessedAtTo = queryParams.lastAccessedAtTo,
            lastReauthenticatedAtFrom = queryParams.lastReauthenticatedAtFrom,
            lastReauthenticatedAtTo = queryParams.lastReauthenticatedAtTo,
            createdAtFrom = queryParams.createdAtFrom,
            createdAtTo = queryParams.createdAtTo,
            updatedAtFrom = queryParams.updatedAtFrom,
            updatedAtTo = queryParams.updatedAtTo,
            isUpdatedAtSet = queryParams.isUpdatedAtSet,
            searchQuery = queryParams.searchQuery
        )

        executeUserSessionsPagedQuery(
            query = query,
            pageNumber = queryParams.listing.pageParams.page,
            pageSize = queryParams.listing.pageParams.size,
            sortBy = queryParams.listing.sortBy,
            sortOrder = queryParams.listing.sortOrder
        )
    } catch (e: ExposedSQLException) {
        AppResult.Error(CommonError.Database("Error executing self user sessions paged query: ${e.message}"))
    }

    private fun Query.applyCommonFilters(
        sessionIds: List<String>,
        identifier: String?,
        identifierIds: List<String>,
        identifierDisplayName: String?,
        identifierAuthProviders: List<UserAuthProvider>,
        excludeIdentifierAuthProviders: List<UserAuthProvider>,
        clientTypes: List<ClientType>,
        excludeClientTypes: List<ClientType>,
        isClientTypeSet: Boolean?,
        deviceId: String?,
        isDeviceIdSet: Boolean?,
        deviceName: String?,
        isDeviceNameSet: Boolean?,
        language: String?,
        isLanguageSet: Boolean?,
        appVersion: String?,
        isAppVersionSet: Boolean?,
        osVersion: String?,
        isOperationSystemVersionSet: Boolean?,
        userAgent: String?,
        isUserAgentSet: Boolean?,
        ipAddress: String?,
        isIpAddressSet: Boolean?,
        expiresAtFrom: KotlinInstant?,
        expiresAtTo: KotlinInstant?,
        lastAccessedAtFrom: KotlinInstant?,
        lastAccessedAtTo: KotlinInstant?,
        lastReauthenticatedAtFrom: KotlinInstant?,
        lastReauthenticatedAtTo: KotlinInstant?,
        createdAtFrom: KotlinInstant?,
        createdAtTo: KotlinInstant?,
        updatedAtFrom: KotlinInstant?,
        updatedAtTo: KotlinInstant?,
        isUpdatedAtSet: Boolean?,
        searchQuery: String?
    ): Query {
        this.applySessionIdentityFilters(sessionIds, identifier, identifierIds, identifierDisplayName)
        this.applyAuthProviderFilters(identifierAuthProviders, excludeIdentifierAuthProviders)
        this.applyClientInfoFilters(
            clientTypes, excludeClientTypes, isClientTypeSet, deviceId, isDeviceIdSet,
            deviceName, isDeviceNameSet, language, isLanguageSet, appVersion, isAppVersionSet,
            osVersion, isOperationSystemVersionSet, userAgent, isUserAgentSet, ipAddress, isIpAddressSet
        )
        this.applySessionTimestampFilters(
            expiresAtFrom, expiresAtTo, lastAccessedAtFrom, lastAccessedAtTo,
            lastReauthenticatedAtFrom, lastReauthenticatedAtTo, createdAtFrom, createdAtTo,
            updatedAtFrom, updatedAtTo, isUpdatedAtSet
        )
        this.applySessionSearchFilter(searchQuery)

        return this
    }

    private fun Query.applySessionIdentityFilters(
        sessionIds: List<String>,
        identifier: String?,
        identifierIds: List<String>,
        identifierDisplayName: String?
    ): Query {
        if (sessionIds.isNotEmpty()) {
            val entityIds = sessionIds.mapNotNull { idStr ->
                runCatching {
                    EntityID(Uuid.parse(idStr), UserSessionsTable)
                }.getOrNull()
            }
            if (entityIds.isNotEmpty()) {
                this.andWhere { UserSessionsTable.id inList entityIds }
            }
        }
        if (!identifier.isNullOrBlank()) {
            this.andWhere { UserSessionsTable.identifier.lowerCase() eq identifier.lowercase() }
        }
        if (identifierIds.isNotEmpty()) {
            val idEntityIds = identifierIds.mapNotNull { idStr ->
                runCatching {
                    EntityID(Uuid.parse(idStr), UserIdentifiersTable)
                }.getOrNull()
            }
            if (idEntityIds.isNotEmpty()) {
                this.andWhere { UserSessionsTable.identifierId inList idEntityIds }
            }
        }
        if (!identifierDisplayName.isNullOrBlank()) {
            this.andWhere {
                UserSessionsTable.identifierDisplayName.lowerCase() eq identifierDisplayName.lowercase()
            }
        }
        return this
    }

    private fun Query.applyAuthProviderFilters(
        identifierAuthProviders: List<UserAuthProvider>,
        excludeIdentifierAuthProviders: List<UserAuthProvider>
    ): Query {
        if (identifierAuthProviders.isNotEmpty()) {
            this.andWhere { UserSessionsTable.identifierAuthProvider inList identifierAuthProviders }
        }
        if (excludeIdentifierAuthProviders.isNotEmpty()) {
            this.andWhere { UserSessionsTable.identifierAuthProvider notInList excludeIdentifierAuthProviders }
        }
        return this
    }

    private fun Query.applyClientInfoFilters(
        clientTypes: List<ClientType>,
        excludeClientTypes: List<ClientType>,
        isClientTypeSet: Boolean?,
        deviceId: String?,
        isDeviceIdSet: Boolean?,
        deviceName: String?,
        isDeviceNameSet: Boolean?,
        language: String?,
        isLanguageSet: Boolean?,
        appVersion: String?,
        isAppVersionSet: Boolean?,
        osVersion: String?,
        isOperationSystemVersionSet: Boolean?,
        userAgent: String?,
        isUserAgentSet: Boolean?,
        ipAddress: String?,
        isIpAddressSet: Boolean?
    ): Query {
        applyClientTypeFilters(clientTypes, excludeClientTypes, isClientTypeSet)
        applyDeviceIdFilters(deviceId, isDeviceIdSet)
        applyDeviceNameFilters(deviceName, isDeviceNameSet)
        applyLanguageFilters(language, isLanguageSet)
        applyAppAndOsVersionFilters(
            appVersion,
            isAppVersionSet,
            osVersion,
            isOperationSystemVersionSet
        )
        applyUserAgentAndIpFilters(
            userAgent,
            isUserAgentSet,
            ipAddress,
            isIpAddressSet
        )
        return this
    }

    private fun Query.applyClientTypeFilters(
        clientTypes: List<ClientType>,
        excludeClientTypes: List<ClientType>,
        isClientTypeSet: Boolean?
    ) {
        if (clientTypes.isNotEmpty()) {
            this.andWhere { UserSessionsTable.clientType inList clientTypes }
        }
        if (excludeClientTypes.isNotEmpty()) {
            this.andWhere { UserSessionsTable.clientType notInList excludeClientTypes }
        }
        if (isClientTypeSet == true) {
            this.andWhere { UserSessionsTable.clientType.isNotNull() }
        } else if (isClientTypeSet == false) {
            this.andWhere { UserSessionsTable.clientType.isNull() }
        }
    }

    private fun Query.applyDeviceIdFilters(
        deviceId: String?,
        isDeviceIdSet: Boolean?
    ) {
        if (!deviceId.isNullOrBlank()) {
            val deviceIdUuid = Uuid.parseOrNull(deviceId)
            if (deviceIdUuid != null) {
                this.andWhere { UserSessionsTable.deviceId eq deviceIdUuid }
            }
        }
        if (isDeviceIdSet == true) {
            this.andWhere { UserSessionsTable.deviceId.isNotNull() }
        } else if (isDeviceIdSet == false) {
            this.andWhere { UserSessionsTable.deviceId.isNull() }
        }
    }

    private fun Query.applyDeviceNameFilters(
        deviceName: String?,
        isDeviceNameSet: Boolean?
    ) {
        if (!deviceName.isNullOrBlank()) {
            this.andWhere { UserSessionsTable.deviceName.lowerCase() eq deviceName.lowercase() }
        }
        if (isDeviceNameSet == true) {
            this.andWhere { UserSessionsTable.deviceName.isNotNull() }
        } else if (isDeviceNameSet == false) {
            this.andWhere { UserSessionsTable.deviceName.isNull() }
        }
    }

    private fun Query.applyLanguageFilters(
        language: String?,
        isLanguageSet: Boolean?
    ) {
        if (!language.isNullOrBlank()) {
            this.andWhere { UserSessionsTable.language.lowerCase() eq language.lowercase() }
        }
        if (isLanguageSet == true) {
            this.andWhere { UserSessionsTable.language.isNotNull() }
        } else if (isLanguageSet == false) {
            this.andWhere { UserSessionsTable.language.isNull() }
        }
    }

    private fun Query.applyAppAndOsVersionFilters(
        appVersion: String?,
        isAppVersionSet: Boolean?,
        osVersion: String?,
        isOperationSystemVersionSet: Boolean?
    ) {
        if (!appVersion.isNullOrBlank()) {
            this.andWhere { UserSessionsTable.appVersion eq appVersion }
        }
        if (isAppVersionSet == true) {
            this.andWhere { UserSessionsTable.appVersion.isNotNull() }
        } else if (isAppVersionSet == false) {
            this.andWhere { UserSessionsTable.appVersion.isNull() }
        }
        if (!osVersion.isNullOrBlank()) {
            this.andWhere { UserSessionsTable.operationSystemVersion eq osVersion }
        }
        if (isOperationSystemVersionSet == true) {
            this.andWhere { UserSessionsTable.operationSystemVersion.isNotNull() }
        } else if (isOperationSystemVersionSet == false) {
            this.andWhere { UserSessionsTable.operationSystemVersion.isNull() }
        }
    }

    private fun Query.applyUserAgentAndIpFilters(
        userAgent: String?,
        isUserAgentSet: Boolean?,
        ipAddress: String?,
        isIpAddressSet: Boolean?
    ) {
        if (!userAgent.isNullOrBlank()) {
            this.andWhere { UserSessionsTable.userAgent.lowerCase() eq userAgent.lowercase() }
        }
        if (isUserAgentSet == true) {
            this.andWhere { UserSessionsTable.userAgent.isNotNull() }
        } else if (isUserAgentSet == false) {
            this.andWhere { UserSessionsTable.userAgent.isNull() }
        }
        if (!ipAddress.isNullOrBlank()) {
            this.andWhere { UserSessionsTable.ipAddress eq ipAddress }
        }
        if (isIpAddressSet == true) {
            this.andWhere { UserSessionsTable.ipAddress.isNotNull() }
        } else if (isIpAddressSet == false) {
            this.andWhere { UserSessionsTable.ipAddress.isNull() }
        }
    }

    private fun Query.applySessionTimestampFilters(
        expiresAtFrom: KotlinInstant?,
        expiresAtTo: KotlinInstant?,
        lastAccessedAtFrom: KotlinInstant?,
        lastAccessedAtTo: KotlinInstant?,
        lastReauthenticatedAtFrom: KotlinInstant?,
        lastReauthenticatedAtTo: KotlinInstant?,
        createdAtFrom: KotlinInstant?,
        createdAtTo: KotlinInstant?,
        updatedAtFrom: KotlinInstant?,
        updatedAtTo: KotlinInstant?,
        isUpdatedAtSet: Boolean?
    ): Query {
        this.applyTimestampFilter(
            column = UserSessionsTable.expiresAt,
            fromInstant = expiresAtFrom,
            toInstant = expiresAtTo
        )
        this.applyTimestampFilter(
            column = UserSessionsTable.lastAccessedAt,
            fromInstant = lastAccessedAtFrom,
            toInstant = lastAccessedAtTo
        )
        this.applyTimestampFilter(
            column = UserSessionsTable.lastReauthenticatedAt,
            fromInstant = lastReauthenticatedAtFrom,
            toInstant = lastReauthenticatedAtTo
        )
        this.applyTimestampFilter(
            column = UserSessionsTable.createdAt,
            fromInstant = createdAtFrom,
            toInstant = createdAtTo
        )
        this.applyTimestampFilter(
            column = UserSessionsTable.updatedAt,
            fromInstant = updatedAtFrom,
            toInstant = updatedAtTo,
            isSet = isUpdatedAtSet
        )
        return this
    }

    private fun Query.applySessionSearchFilter(searchQuery: String?): Query {
        if (!searchQuery.isNullOrBlank()) {
            val pattern = substringSqlLikePattern(searchQuery.lowercase())
            this.andWhere {
                (UserSessionsTable.identifier.lowerCase() like pattern) or
                    (UserSessionsTable.identifierDisplayName.lowerCase() like pattern) or
                    (UserSessionsTable.deviceName.lowerCase() like pattern)
            }
        }
        return this
    }

    private fun executeUserSessionsPagedQuery(
        query: Query,
        pageNumber: Int,
        pageSize: Int,
        sortBy: UserSessionSortBy,
        sortOrder: SortOrder
    ): AppResult<PagedResult<UserSessionInternal>> {
        val totalCount = query.count()
        val totalPages = getNumOfTotalPages(
            totalCount,
            pageSize
        )

        val sortColumn = when (sortBy) {
            UserSessionSortBy.EXPIRES_AT -> UserSessionsTable.expiresAt
            UserSessionSortBy.LAST_ACCESSED_AT -> UserSessionsTable.lastAccessedAt
            UserSessionSortBy.LAST_REAUTHENTICATED_AT -> UserSessionsTable.lastReauthenticatedAt
            UserSessionSortBy.CREATED_AT -> UserSessionsTable.createdAt
            UserSessionSortBy.UPDATED_AT -> UserSessionsTable.updatedAt
        }

        val pageParams = PageParams(
            page = pageNumber,
            size = pageSize
        )

        val items = query.orderBy(sortColumn to sortOrder.toExposedSortOrder()).applyPagination(pageParams)
            .map { it.toUserSessionInternal() }

        return AppResult.Success(
            PagedResult(
                items = items,
                totalCount = totalCount,
                pageNumber = pageNumber,
                pageSize = pageSize,
                totalPages = totalPages
            )
        )
    }

    private fun ResultRow.toUserSessionInternal(): UserSessionInternal = UserSessionInternal(
        id = UserSessionId(this[UserSessionsTable.id].value),
        userId = UserId(this[UserSessionsTable.userId].value),
        userRole = this[UserSessionsTable.userRole],
        identifierId = UserIdentifierId(this[UserSessionsTable.identifierId].value),
        identifierAuthProvider = this[UserSessionsTable.identifierAuthProvider],
        identifier = this[UserSessionsTable.identifier],
        identifierDisplayName = this[UserSessionsTable.identifierDisplayName],
        refreshTokenHash = RefreshTokenHash(this[UserSessionsTable.refreshTokenHash]),
        clientDeviceInfo = ClientDeviceInfo(
            clientType = this[UserSessionsTable.clientType],
            deviceId = this[UserSessionsTable.deviceId]?.let { ClientDeviceId(it) },
            deviceName = this[UserSessionsTable.deviceName],
            appVersion = this[UserSessionsTable.appVersion],
            operationSystemVersion = this[UserSessionsTable.operationSystemVersion],
            language = this[UserSessionsTable.language]
        ),
        userAgent = this[UserSessionsTable.userAgent],
        ipAddress = this[UserSessionsTable.ipAddress],
        expiresAt = this[UserSessionsTable.expiresAt].toKotlinInstant(),
        lastAccessedAt = this[UserSessionsTable.lastAccessedAt].toKotlinInstant(),
        lastReauthenticatedAt = this[UserSessionsTable.lastReauthenticatedAt].toKotlinInstant(),
        createdAt = this[UserSessionsTable.createdAt].toKotlinInstant(),
        updatedAt = this[UserSessionsTable.updatedAt]?.toKotlinInstant()
    )
}
