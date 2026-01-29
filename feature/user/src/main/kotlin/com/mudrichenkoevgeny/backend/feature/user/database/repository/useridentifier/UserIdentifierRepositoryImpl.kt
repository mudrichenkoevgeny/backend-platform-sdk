package io.github.mudrichenkoevgeny.backend.feature.user.database.repository.useridentifier

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.listing.pagination.model.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.listing.pagination.model.PagedResponse
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.util.CollectionUtils.isAllArgsNull
import io.github.mudrichenkoevgeny.backend.core.database.extensions.applyPagination
import io.github.mudrichenkoevgeny.backend.feature.user.database.table.UserIdentifiersTable
import io.github.mudrichenkoevgeny.backend.feature.user.enums.UserAuthProvider
import io.github.mudrichenkoevgeny.backend.core.common.model.UserId
import io.github.mudrichenkoevgeny.backend.feature.user.model.useridentifier.UserIdentifier
import io.github.mudrichenkoevgeny.backend.core.common.model.UserIdentifierId
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
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
class UserIdentifierRepositoryImpl @Inject constructor() : UserIdentifierRepository {

    override suspend fun createUserIdentifier(
        userIdentifier: UserIdentifier
    ): AppResult<UserIdentifier> {
        val inserted = UserIdentifiersTable.insert { userIdentifierRow ->
            userIdentifierRow[id] = userIdentifier.id.value
            userIdentifierRow[userId] = userIdentifier.userId.value
            userIdentifierRow[userAuthProvider] = userIdentifier.userAuthProvider
            userIdentifierRow[identifier] = userIdentifier.identifier
            userIdentifierRow[passwordHash] = userIdentifier.passwordHash
            userIdentifierRow[createdAt] = userIdentifier.createdAt
            userIdentifierRow[updatedAt] = userIdentifier.updatedAt
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

    override suspend fun deleteAllUserIdentifierByUserId(
        userId: UserId
    ): AppResult<Unit> {
        UserIdentifiersTable.deleteWhere { UserIdentifiersTable.userId eq userId.value }

        return AppResult.Success(Unit)
    }

    override suspend fun updateUserIdentifier(
        userIdentifier: UserIdentifier,
        identifier: String?,
        passwordHash: String?
    ): AppResult<UserIdentifier> {
        if (isAllArgsNull(identifier, passwordHash)) {
            return AppResult.Success(userIdentifier)
        }

        val updatedAt = Instant.now()

        val updatedRows = UserIdentifiersTable.update({ UserIdentifiersTable.id eq userIdentifier.id.value}) {
            if (identifier != null) {
                it[UserIdentifiersTable.identifier] = identifier
            }

            if (passwordHash != null) {
                it[UserIdentifiersTable.passwordHash] = passwordHash
            }

            it[UserIdentifiersTable.updatedAt] = updatedAt
        }

        if (updatedRows == 0) {
            return AppResult.Error(
                CommonError.Database(
                    "Failed to update fields for userIdentifier id=${userIdentifier.id.value}"
                )
            )
        }

        return AppResult.Success(
            userIdentifier.copy(
                identifier = identifier ?: userIdentifier.identifier,
                passwordHash = passwordHash ?: userIdentifier.passwordHash,
                updatedAt = updatedAt
            )
        )
    }

    override suspend fun getUserIdentifierById(
        userIdentifierId: UserIdentifierId
    ): AppResult<UserIdentifier?> {
        val resultRow = UserIdentifiersTable
            .selectAll()
            .where { UserIdentifiersTable.id eq userIdentifierId.value }
            .limit(1)
            .singleOrNull()

        return AppResult.Success(resultRow?.toUserIdentifier())
    }

    override suspend fun getUserIdentifiersListByUserId(
        userId: UserId,
        userAuthProvider: UserAuthProvider?
    ): AppResult<List<UserIdentifier>> {
        var query = UserIdentifiersTable
            .selectAll()
            .where { UserIdentifiersTable.userId eq userId.value }

        if (userAuthProvider != null) {
            query = query.andWhere { UserIdentifiersTable.userAuthProvider eq userAuthProvider }
        }

        val userIdentifiers = query.map { it.toUserIdentifier() }

        return AppResult.Success(userIdentifiers)
    }

    override suspend fun getUserIdentifier(
        userAuthProvider: UserAuthProvider,
        identifier: String
    ): AppResult<UserIdentifier?> {
        val query = UserIdentifiersTable
            .selectAll()
            .where {
                (UserIdentifiersTable.identifier eq identifier) and
                        (UserIdentifiersTable.userAuthProvider eq userAuthProvider)
            }
            .limit(1)
            .singleOrNull()

        return AppResult.Success(query?.toUserIdentifier())
    }

    override suspend fun getUserIdentifiersList(
        params: PageParams,
        userAuthProvider: UserAuthProvider?
    ): AppResult<PagedResponse<UserIdentifier>> {
        val query = if (userAuthProvider != null) {
            UserIdentifiersTable.selectAll().where { UserIdentifiersTable.userAuthProvider eq userAuthProvider }
        } else {
            UserIdentifiersTable.selectAll()
        }

        val totalCount = query.count()

        val userIdentifiers = query
            .applyPagination(params)
            .map { it.toUserIdentifier() }

        return AppResult.Success(
            PagedResponse(
                items = userIdentifiers,
                totalCount = totalCount,
                page = params.page,
                size = params.size
            )
        )
    }

    private fun ResultRow.toUserIdentifier(): UserIdentifier = UserIdentifier(
        id = UserIdentifierId(this[UserIdentifiersTable.id].value),
        userId = UserId(this[UserIdentifiersTable.userId].value),
        userAuthProvider = this[UserIdentifiersTable.userAuthProvider],
        identifier = this[UserIdentifiersTable.identifier],
        passwordHash = this[UserIdentifiersTable.passwordHash],
        createdAt = this[UserIdentifiersTable.createdAt],
        updatedAt = this[UserIdentifiersTable.updatedAt]
    )
}