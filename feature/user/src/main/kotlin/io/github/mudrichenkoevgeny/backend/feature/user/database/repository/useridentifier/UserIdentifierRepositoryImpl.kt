package io.github.mudrichenkoevgeny.backend.feature.user.database.repository.useridentifier

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
import io.github.mudrichenkoevgeny.backend.feature.user.database.table.UserIdentifiersTable
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.UserRoleAccessFilter
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.SortOrder
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.passwordhash.PasswordHash
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifier
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierInternal
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserSortValues
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Instant as JavaInstant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default [UserIdentifierRepository] implementation backed by Exposed and [UserIdentifiersTable].
 *
 * Performs synchronous Exposed DSL operations and maps [ResultRow] values into [UserIdentifierInternal] models.
 * Returns [CommonError.Database] when an operation reports no affected rows.
 * Supports role-scoped listing through [UserRoleAccessFilter] plus multi-value filters for management APIs.
 */
@Singleton
class UserIdentifierRepositoryImpl @Inject constructor() : UserIdentifierRepository {

    override suspend fun createUserIdentifier(
        userIdentifier: UserIdentifierInternal
    ): AppResult<UserIdentifierInternal> {
        val inserted = UserIdentifiersTable.insert { userIdentifierRow ->
            userIdentifierRow[id] = userIdentifier.id.value
            userIdentifierRow[userId] = userIdentifier.userId.value
            userIdentifierRow[userAuthProvider] = userIdentifier.userAuthProvider
            userIdentifierRow[identifier] = userIdentifier.identifier
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

        return AppResult.Success(userIdentifier)
    }

    override suspend fun deleteUserIdentifier(
        userIdentifierId: UserIdentifierId
    ): AppResult<Unit>  {
        val deletedRows = UserIdentifiersTable
            .deleteWhere { UserIdentifiersTable.id eq userIdentifierId.value }

        if (deletedRows == 0) {
            return AppResult.Error(
                CommonError.Database(
                    "Failed to delete userIdentifier: id=${userIdentifierId.value}"
                )
            )
        }

        return AppResult.Success(Unit)
    }

    override suspend fun updatePasswordHash(
        userIdentifier: UserIdentifierInternal,
        newPasswordHash: PasswordHash
    ): AppResult<UserIdentifierInternal> {
        val updatedAt = JavaInstant.now()

        val updatedRows = UserIdentifiersTable.update(
            { UserIdentifiersTable.id eq userIdentifier.id.value }
        ) {
            it[UserIdentifiersTable.passwordHash] = newPasswordHash.value
            it[UserIdentifiersTable.updatedAt] = updatedAt
        }

        if (updatedRows == 0) {
            return AppResult.Error(
                CommonError.Database(
                    "Failed to update password hash for userIdentifier id=${userIdentifier.id.value}"
                )
            )
        }

        return AppResult.Success(
            userIdentifier.copy(
                passwordHash = newPasswordHash,
                updatedAt = updatedAt.toKotlinInstant()
            )
        )
    }

    override suspend fun getUserIdentifierInternalById(
        userIdentifierId: UserIdentifierId
    ): AppResult<UserIdentifierInternal?> {
        return AppResult.Success(getIdentifierResultRow(userIdentifierId)?.toUserIdentifierInternal())
    }

    override suspend fun getUserIdentifierById(
        userIdentifierId: UserIdentifierId
    ): AppResult<UserIdentifier?> {
        return AppResult.Success(getIdentifierResultRow(userIdentifierId)?.toUserIdentifier())
    }

    private fun getIdentifierResultRow(
        userIdentifierId: UserIdentifierId
    ): ResultRow? {
        return UserIdentifiersTable
            .selectAll()
            .where { UserIdentifiersTable.id eq userIdentifierId.value }
            .limit(1)
            .singleOrNull()
    }

    override suspend fun getUserIdentifiersListByUserId(
        userId: UserId,
        userAuthProvider: UserAuthProvider?
    ): AppResult<List<UserIdentifierInternal>> {
        var query = UserIdentifiersTable
            .selectAll()
            .where { UserIdentifiersTable.userId eq userId.value }

        if (userAuthProvider != null) {
            query = query.andWhere { UserIdentifiersTable.userAuthProvider eq userAuthProvider }
        }

        val userIdentifiers = query.map { it.toUserIdentifierInternal() }

        return AppResult.Success(userIdentifiers)
    }

    override suspend fun getUserIdentifierInternalByProvider(
        userAuthProvider: UserAuthProvider,
        identifier: String
    ): AppResult<UserIdentifierInternal?> {
        return AppResult.Success(
            getUserIdentifierByProviderResultRow(userAuthProvider, identifier)?.toUserIdentifierInternal()
        )
    }

    private fun getUserIdentifierByProviderResultRow(
        userAuthProvider: UserAuthProvider,
        identifier: String
    ): ResultRow? {
        return UserIdentifiersTable
            .selectAll()
            .where {
                (UserIdentifiersTable.identifier eq identifier) and
                        (UserIdentifiersTable.userAuthProvider eq userAuthProvider)
            }
            .limit(1)
            .singleOrNull()
    }

    override suspend fun getUserIdentifiersPageWithAccessFilter(
        accessFilter: UserRoleAccessFilter,
        params: PageParams,
        sortBy: UserSortValues.UserIdentifierSortBy,
        sortOrder: SortOrder,
        userIds: List<UserId>,
        userAuthProviders: List<UserAuthProvider>,
        identifiers: List<String>
    ): AppResult<PagedResult<UserIdentifier>> {
        var query = UserIdentifiersTable.selectAll()

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
                UserIdentifiersTable.userId inList allowedUserIds
            }
        }

        if (userIds.isNotEmpty()) {
            query = query.andWhere { UserIdentifiersTable.userId inList userIds.map { it.value } }
        }

        return executeUserIdentifiersPagedQuery(
            query = query,
            params = params,
            sortBy = sortBy,
            sortOrder = sortOrder,
            userAuthProviders = userAuthProviders,
            identifiers = identifiers
        )
    }

    override suspend fun getUserIdentifiersPageByUserId(
        userId: UserId,
        params: PageParams,
        sortBy: UserSortValues.UserIdentifierSortBy,
        sortOrder: SortOrder,
        userAuthProviders: List<UserAuthProvider>,
        identifiers: List<String>
    ): AppResult<PagedResult<UserIdentifier>> {
        val query = UserIdentifiersTable
            .selectAll()
            .where { UserIdentifiersTable.userId eq userId.value }

        return executeUserIdentifiersPagedQuery(
            query = query,
            params = params,
            sortBy = sortBy,
            sortOrder = sortOrder,
            userAuthProviders = userAuthProviders,
            identifiers = identifiers
        )
    }

    private fun executeUserIdentifiersPagedQuery(
        query: Query,
        params: PageParams,
        sortBy: UserSortValues.UserIdentifierSortBy,
        sortOrder: SortOrder,
        userAuthProviders: List<UserAuthProvider>,
        identifiers: List<String>
    ): AppResult<PagedResult<UserIdentifier>> {
        if (userAuthProviders.isNotEmpty()) {
            query.andWhere { UserIdentifiersTable.userAuthProvider inList userAuthProviders }
        }

        val nonBlankIdentifiers = identifiers.filter { it.isNotBlank() }
        if (nonBlankIdentifiers.isNotEmpty()) {
            val identifierPredicate: Op<Boolean> = nonBlankIdentifiers
                .map { needle ->
                    (UserIdentifiersTable.identifier.lowerCase() like substringSqlLikePattern(needle.lowercase()))
                            as Op<Boolean>
                }
                .reduce { acc, condition -> acc or condition }
            query.andWhere { identifierPredicate }
        }

        val totalCount = query.count()
        val totalPages = getNumOfTotalPages(totalCount, params.size)

        val sortColumn = when (sortBy) {
            UserSortValues.UserIdentifierSortBy.CREATED_AT -> UserIdentifiersTable.createdAt
            UserSortValues.UserIdentifierSortBy.UPDATED_AT -> UserIdentifiersTable.updatedAt
        }

        val items = query
            .orderBy(sortColumn to sortOrder.toExposedSortOrder())
            .applyPagination(params)
            .map { it.toUserIdentifier() }

        return AppResult.Success(
            PagedResult(
                items = items,
                totalCount = totalCount,
                pageNumber = params.page,
                pageSize = params.size,
                totalPages = totalPages
            )
        )
    }

    private fun ResultRow.toUserIdentifierInternal(): UserIdentifierInternal = UserIdentifierInternal(
        id = UserIdentifierId(this[UserIdentifiersTable.id].value),
        userId = UserId(this[UserIdentifiersTable.userId].value),
        userAuthProvider = this[UserIdentifiersTable.userAuthProvider],
        identifier = this[UserIdentifiersTable.identifier],
        externalProviderEmail = this[UserIdentifiersTable.externalProviderEmail],
        passwordHash = this[UserIdentifiersTable.passwordHash]?.let { passwordHash ->
            PasswordHash(passwordHash)
        },
        createdAt = this[UserIdentifiersTable.createdAt].toKotlinInstant(),
        updatedAt = this[UserIdentifiersTable.updatedAt]?.toKotlinInstant()
    )

    private fun ResultRow.toUserIdentifier(): UserIdentifier = UserIdentifier(
        id = UserIdentifierId(this[UserIdentifiersTable.id].value),
        userId = UserId(this[UserIdentifiersTable.userId].value),
        userAuthProvider = this[UserIdentifiersTable.userAuthProvider],
        identifier = this[UserIdentifiersTable.identifier],
        externalProviderEmail = this[UserIdentifiersTable.externalProviderEmail],
        isSensitiveValuesMasked = false,
        createdAt = this[UserIdentifiersTable.createdAt].toKotlinInstant(),
        updatedAt = this[UserIdentifiersTable.updatedAt]?.toKotlinInstant()
    )
}