package io.github.mudrichenkoevgeny.backend.feature.user.database.repository.useridentifier

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
import io.github.mudrichenkoevgeny.backend.feature.user.database.table.UsersTable
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.UserRoleAccessFilter
import io.github.mudrichenkoevgeny.backend.feature.user.network.model.ManagementIdentifierListQueryParams
import io.github.mudrichenkoevgeny.backend.feature.user.network.model.SelfIdentifierListQueryParams
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.SortOrder
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.passwordhash.PasswordHash
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierInternal
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserIdentifierSortBy
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.inSubQuery
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.notInList
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.uuid.Uuid
import java.time.Instant as JavaInstant
import kotlin.time.Instant as KotlinInstant

/**
 * Default [UserIdentifierRepository] implementation backed by Exposed and [UserIdentifiersTable].
 */
@Singleton
public class UserIdentifierRepositoryImpl @Inject constructor() : UserIdentifierRepository {
    override suspend fun createUserIdentifier(
        userIdentifier: UserIdentifierInternal
    ): AppResult<UserIdentifierInternal> {
        return try {
            val inserted = UserIdentifiersTable.insert { userIdentifierRow ->
                userIdentifierRow[id] = userIdentifier.id.value
                userIdentifierRow[userId] = userIdentifier.userId.value
                userIdentifierRow[userAuthProvider] = userIdentifier.userAuthProvider
                userIdentifierRow[identifier] = userIdentifier.identifier
                userIdentifierRow[displayName] = userIdentifier.displayName
                userIdentifierRow[externalProviderEmail] = userIdentifier.externalProviderEmail
                userIdentifierRow[passwordHash] = userIdentifier.passwordHash?.value
                userIdentifierRow[createdAt] = userIdentifier.createdAt.toJavaInstant()
                userIdentifierRow[updatedAt] = userIdentifier.updatedAt?.toJavaInstant()
            }

            if (inserted.insertedCount == 0) {
                return AppResult.Error(
                    CommonError.Database(
                        "UserIdentifier creation failed for userId=${userIdentifier.userId.value}, " +
                            "userAuthProvider=${userIdentifier.userAuthProvider}"
                    )
                )
            }

            AppResult.Success(userIdentifier)
        } catch (e: ExposedSQLException) {
            AppResult.Error(
                CommonError.Database("Error creating user identifier: ${e.message}")
            )
        }
    }

    override suspend fun deleteUserIdentifier(userIdentifierId: UserIdentifierId): AppResult<Unit> {
        return try {
            val deletedRows = UserIdentifiersTable.deleteWhere { UserIdentifiersTable.id eq userIdentifierId.value }

            if (deletedRows == 0) {
                return AppResult.Error(
                    CommonError.Database(
                        "Failed to delete userIdentifier: id=${userIdentifierId.value}"
                    )
                )
            }

            AppResult.Success(Unit)
        } catch (e: ExposedSQLException) {
            AppResult.Error(
                CommonError.Database("Error deleting user identifier: ${e.message}")
            )
        }
    }

    override suspend fun updatePasswordHash(
        userIdentifier: UserIdentifierInternal,
        newPasswordHash: PasswordHash?
    ): AppResult<UserIdentifierInternal> {
        return try {
            val updatedAt = JavaInstant.now()

            val updatedRows = UserIdentifiersTable.update(
                { UserIdentifiersTable.id eq userIdentifier.id.value }
            ) {
                it[UserIdentifiersTable.passwordHash] = newPasswordHash?.value
                it[UserIdentifiersTable.updatedAt] = updatedAt
            }

            if (updatedRows == 0) {
                return AppResult.Error(
                    CommonError.Database(
                        "Failed to update password hash for userIdentifier id=${userIdentifier.id.value}"
                    )
                )
            }

            AppResult.Success(
                userIdentifier.copy(
                    passwordHash = newPasswordHash,
                    updatedAt = updatedAt.toKotlinInstant()
                )
            )
        } catch (e: ExposedSQLException) {
            AppResult.Error(
                CommonError.Database("Error updating password hash: ${e.message}")
            )
        }
    }

    override suspend fun getUserIdentifierInternalById(
        userIdentifierId: UserIdentifierId
    ): AppResult<UserIdentifierInternal?> = try {
        AppResult.Success(
            getIdentifierResultRow(userIdentifierId)?.toUserIdentifierInternal()
        )
    } catch (e: ExposedSQLException) {
        AppResult.Error(
            CommonError.Database("Error retrieving user identifier by id: ${e.message}")
        )
    }

    private fun getIdentifierResultRow(userIdentifierId: UserIdentifierId): ResultRow? =
        UserIdentifiersTable.selectAll().where { UserIdentifiersTable.id eq userIdentifierId.value }.limit(1)
            .singleOrNull()

    override suspend fun getUserIdentifiersListByUserId(
        userId: UserId,
        userAuthProvider: UserAuthProvider?
    ): AppResult<List<UserIdentifierInternal>> = try {
        var query = UserIdentifiersTable.selectAll().where { UserIdentifiersTable.userId eq userId.value }

        if (userAuthProvider != null) {
            query = query.andWhere {
                UserIdentifiersTable.userAuthProvider eq userAuthProvider
            }
        }

        val userIdentifiers = query.map { it.toUserIdentifierInternal() }

        AppResult.Success(userIdentifiers)
    } catch (e: ExposedSQLException) {
        AppResult.Error(
            CommonError.Database("Error retrieving user identifiers by user id: ${e.message}")
        )
    }

    override suspend fun getUserIdentifierInternalByProvider(
        userAuthProvider: UserAuthProvider,
        identifier: String
    ): AppResult<UserIdentifierInternal?> = try {
        AppResult.Success(
            getUserIdentifierByProviderResultRow(
                userAuthProvider,
                identifier
            )?.toUserIdentifierInternal()
        )
    } catch (e: ExposedSQLException) {
        AppResult.Error(
            CommonError.Database("Error retrieving user identifier by provider: ${e.message}")
        )
    }

    private fun getUserIdentifierByProviderResultRow(
        userAuthProvider: UserAuthProvider,
        identifier: String
    ): ResultRow? = UserIdentifiersTable.selectAll().where {
        (UserIdentifiersTable.identifier eq identifier) and (UserIdentifiersTable.userAuthProvider eq userAuthProvider)
    }.limit(1).singleOrNull()

    override suspend fun getUserIdentifiersPageWithAccessFilter(
        accessFilter: UserRoleAccessFilter,
        queryParams: ManagementIdentifierListQueryParams
    ): AppResult<PagedResult<UserIdentifierInternal>> = try {
        var query = UserIdentifiersTable.selectAll()

        query = query.andWhere {
            val allowedRoleConditions =
                accessFilter.allowedUserRoles.map { allowedRole -> UsersTable.role eq allowedRole }

            val allowedRolesPredicate = allowedRoleConditions.reduceOrNull { acc, condition -> acc or condition }
                ?: Op.FALSE

            val allowedUsersSubQuery = UsersTable.select(UsersTable.id).where { allowedRolesPredicate }

            UserIdentifiersTable.userId inSubQuery allowedUsersSubQuery
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
                query = query.andWhere {
                    UserIdentifiersTable.userId inList userEntityIds
                }
            }
        }

        query = query.applyCommonFilters(
            identifierIds = queryParams.identifierIds,
            userAuthProviders = queryParams.userAuthProviders,
            excludeUserAuthProviders = queryParams.excludeUserAuthProviders,
            identifier = queryParams.identifier,
            displayName = queryParams.displayName,
            externalProviderEmail = queryParams.externalProviderEmail,
            isExternalProviderEmailSet = queryParams.isExternalProviderEmailSet,
            createdAtFrom = queryParams.createdAtFrom,
            createdAtTo = queryParams.createdAtTo,
            updatedAtFrom = queryParams.updatedAtFrom,
            updatedAtTo = queryParams.updatedAtTo,
            isUpdatedAtSet = queryParams.isUpdatedAtSet,
            searchQuery = queryParams.searchQuery
        )

        executeUserIdentifiersPagedQuery(
            query = query,
            pageNumber = queryParams.listing.pageParams.page,
            pageSize = queryParams.listing.pageParams.size,
            sortBy = queryParams.listing.sortBy,
            sortOrder = queryParams.listing.sortOrder
        )
    } catch (e: ExposedSQLException) {
        AppResult.Error(
            CommonError.Database("Error executing user identifiers paged query: ${e.message}")
        )
    }

    override suspend fun getUserIdentifiersPageByUserId(
        userId: UserId,
        queryParams: SelfIdentifierListQueryParams
    ): AppResult<PagedResult<UserIdentifierInternal>> = try {
        var query = UserIdentifiersTable.selectAll().where { UserIdentifiersTable.userId eq userId.value }

        query = query.applyCommonFilters(
            identifierIds = queryParams.identifierIds,
            userAuthProviders = queryParams.userAuthProviders,
            excludeUserAuthProviders = queryParams.excludeUserAuthProviders,
            identifier = queryParams.identifier,
            displayName = queryParams.displayName,
            externalProviderEmail = queryParams.externalProviderEmail,
            isExternalProviderEmailSet = queryParams.isExternalProviderEmailSet,
            createdAtFrom = queryParams.createdAtFrom,
            createdAtTo = queryParams.createdAtTo,
            updatedAtFrom = queryParams.updatedAtFrom,
            updatedAtTo = queryParams.updatedAtTo,
            isUpdatedAtSet = queryParams.isUpdatedAtSet,
            searchQuery = queryParams.searchQuery
        )

        executeUserIdentifiersPagedQuery(
            query = query,
            pageNumber = queryParams.listing.pageParams.page,
            pageSize = queryParams.listing.pageParams.size,
            sortBy = queryParams.listing.sortBy,
            sortOrder = queryParams.listing.sortOrder
        )
    } catch (e: ExposedSQLException) {
        AppResult.Error(
            CommonError.Database("Error executing self user identifiers paged query: ${e.message}")
        )
    }

    private fun Query.applyCommonFilters(
        identifierIds: List<String>,
        userAuthProviders: List<UserAuthProvider>,
        excludeUserAuthProviders: List<UserAuthProvider>,
        identifier: String?,
        displayName: String?,
        externalProviderEmail: String?,
        isExternalProviderEmailSet: Boolean?,
        createdAtFrom: KotlinInstant?,
        createdAtTo: KotlinInstant?,
        updatedAtFrom: KotlinInstant?,
        updatedAtTo: KotlinInstant?,
        isUpdatedAtSet: Boolean?,
        searchQuery: String?
    ): Query {
        var query = this

        if (identifierIds.isNotEmpty()) {
            val entityIds = identifierIds.mapNotNull { idStr ->
                runCatching {
                    EntityID(
                        Uuid.parse(idStr),
                        UserIdentifiersTable
                    )
                }.getOrNull()
            }
            if (entityIds.isNotEmpty()) {
                query = query.andWhere { UserIdentifiersTable.id inList entityIds }
            }
        }

        if (userAuthProviders.isNotEmpty()) {
            query = query.andWhere {
                UserIdentifiersTable.userAuthProvider inList userAuthProviders
            }
        }
        if (excludeUserAuthProviders.isNotEmpty()) {
            query = query.andWhere {
                UserIdentifiersTable.userAuthProvider notInList excludeUserAuthProviders
            }
        }

        if (!identifier.isNullOrBlank()) {
            query = query.andWhere {
                UserIdentifiersTable.identifier.lowerCase() eq identifier.lowercase()
            }
        }
        if (!displayName.isNullOrBlank()) {
            query = query.andWhere {
                UserIdentifiersTable.displayName.lowerCase() eq displayName.lowercase()
            }
        }
        if (!externalProviderEmail.isNullOrBlank()) {
            query = query.andWhere {
                UserIdentifiersTable.externalProviderEmail.lowerCase() eq externalProviderEmail.lowercase()
            }
        }
        if (isExternalProviderEmailSet == true) {
            query = query.andWhere { UserIdentifiersTable.externalProviderEmail.isNotNull() }
        } else if (isExternalProviderEmailSet == false) {
            query = query.andWhere { UserIdentifiersTable.externalProviderEmail.isNull() }
        }

        query.applyTimestampFilter(
            column = UserIdentifiersTable.createdAt,
            fromInstant = createdAtFrom,
            toInstant = createdAtTo
        )
        query.applyTimestampFilter(
            column = UserIdentifiersTable.updatedAt,
            fromInstant = updatedAtFrom,
            toInstant = updatedAtTo,
            isSet = isUpdatedAtSet
        )

        if (!searchQuery.isNullOrBlank()) {
            val pattern = substringSqlLikePattern(searchQuery.lowercase())
            query = query.andWhere {
                (UserIdentifiersTable.identifier.lowerCase() like pattern) or
                    (UserIdentifiersTable.displayName.lowerCase() like pattern) or
                    (UserIdentifiersTable.externalProviderEmail.lowerCase() like pattern)
            }
        }

        return query
    }

    private fun executeUserIdentifiersPagedQuery(
        query: Query,
        pageNumber: Int,
        pageSize: Int,
        sortBy: UserIdentifierSortBy,
        sortOrder: SortOrder
    ): AppResult<PagedResult<UserIdentifierInternal>> {
        val totalCount = query.count()
        val totalPages = getNumOfTotalPages(
            totalCount,
            pageSize
        )

        val sortColumn = when (sortBy) {
            UserIdentifierSortBy.CREATED_AT -> UserIdentifiersTable.createdAt
            UserIdentifierSortBy.UPDATED_AT -> UserIdentifiersTable.updatedAt
            UserIdentifierSortBy.IDENTIFIER -> UserIdentifiersTable.identifier
            UserIdentifierSortBy.DISPLAY_NAME -> UserIdentifiersTable.displayName
        }

        val pageParams = PageParams(
            page = pageNumber,
            size = pageSize
        )

        val items = query.orderBy(sortColumn to sortOrder.toExposedSortOrder()).applyPagination(pageParams)
            .map { it.toUserIdentifierInternal() }

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

    private fun ResultRow.toUserIdentifierInternal(): UserIdentifierInternal = UserIdentifierInternal(
        id = UserIdentifierId(this[UserIdentifiersTable.id].value),
        userId = UserId(this[UserIdentifiersTable.userId].value),
        userAuthProvider = this[UserIdentifiersTable.userAuthProvider],
        identifier = this[UserIdentifiersTable.identifier],
        displayName = this[UserIdentifiersTable.displayName],
        externalProviderEmail = this[UserIdentifiersTable.externalProviderEmail],
        passwordHash = this[UserIdentifiersTable.passwordHash]?.let { passwordHash ->
            PasswordHash(passwordHash)
        },
        createdAt = this[UserIdentifiersTable.createdAt].toKotlinInstant(),
        updatedAt = this[UserIdentifiersTable.updatedAt]?.toKotlinInstant()
    )
}
