package com.mudrichenkoevgeny.backend.feature.user.database.repository.user

import com.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import com.mudrichenkoevgeny.backend.core.common.listing.pagination.model.PageParams
import com.mudrichenkoevgeny.backend.core.common.listing.pagination.model.PagedResponse
import com.mudrichenkoevgeny.backend.core.common.result.AppResult
import com.mudrichenkoevgeny.backend.core.common.util.CollectionUtils.isAllArgsNull
import com.mudrichenkoevgeny.backend.core.database.extensions.applyPagination
import com.mudrichenkoevgeny.backend.feature.user.model.user.User
import com.mudrichenkoevgeny.backend.feature.user.database.table.UsersTable
import com.mudrichenkoevgeny.backend.feature.user.enums.UserAccountStatus
import com.mudrichenkoevgeny.backend.feature.user.enums.UserRole
import com.mudrichenkoevgeny.backend.core.common.model.UserId
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor() : UserRepository {

    override suspend fun createUser(
        user: User
    ): AppResult<User> {
        val inserted = UsersTable.insert { userRow ->
            userRow[id] = user.id.value
            userRow[role] = user.role
            userRow[accountStatus] = user.accountStatus
            userRow[lastLoginAt] = user.lastLoginAt
            userRow[lastActiveAt] = user.lastActiveAt
            userRow[createdAt] = user.createdAt
            userRow[updatedAt] = user.updatedAt
        }

        if (inserted.insertedCount == 0) {
            return AppResult.Error(
                CommonError.Database("User creation failed for id=${user.id.value}")
            )
        }

        return AppResult.Success(user)
    }

    override suspend fun deleteUser(
        userId: UserId
    ): AppResult<Unit> {
        UsersTable.deleteWhere { UsersTable.id eq userId.value }

        return AppResult.Success(Unit)
    }

    override suspend fun updateUser(
        user: User,
        status: UserAccountStatus?,
        lastLoginAt: Instant?,
        lastActiveAt: Instant?
    ): AppResult<User> {
        if (isAllArgsNull(status, lastLoginAt, lastActiveAt)) {
            return AppResult.Success(user)
        }

        val updatedAt = Instant.now()

        val updatedRows = UsersTable.update({ UsersTable.id eq user.id.value }) {
            if (status != null) {
                it[UsersTable.accountStatus] = status
            }

            if (lastLoginAt != null) {
                it[UsersTable.lastLoginAt] = lastLoginAt
            }

            if (lastActiveAt != null) {
                it[UsersTable.lastActiveAt] = lastActiveAt
            }

            it[UsersTable.updatedAt] = updatedAt
        }

        if (updatedRows == 0) {
            return AppResult.Error(
                CommonError.Database("Failed to update fields for user id=${user.id.value}")
            )
        }

        return AppResult.Success(
            user.copy(
                accountStatus = status ?: user.accountStatus,
                lastLoginAt = lastLoginAt ?: user.lastLoginAt,
                lastActiveAt = lastActiveAt ?: user.lastActiveAt,
                updatedAt = updatedAt
            )
        )
    }

    override suspend fun getUserById(
        userId: UserId
    ): AppResult<User?> {
        val resultRow = UsersTable
            .selectAll()
            .where { UsersTable.id eq userId.value }
            .singleOrNull()

        return AppResult.Success(resultRow?.toUser())
    }

    override suspend fun getUsersList(
        params: PageParams,
        role: UserRole?,
        accountStatus: UserAccountStatus?
    ): AppResult<PagedResponse<User>> {
        val query = UsersTable.selectAll()

        role?.let { r -> query.andWhere { UsersTable.role eq r } }
        accountStatus?.let { status -> query.andWhere { UsersTable.accountStatus eq status } }

        val totalCount = query.count()

        val users = query
            .applyPagination(params)
            .map { it.toUser() }

        return AppResult.Success(
            PagedResponse(
                items = users,
                totalCount = totalCount,
                page = params.page,
                size = params.size
            )
        )
    }

    private fun ResultRow.toUser(): User = User(
        id = UserId(this[UsersTable.id].value),
        role = this[UsersTable.role],
        accountStatus = this[UsersTable.accountStatus],
        lastLoginAt = this[UsersTable.lastLoginAt],
        lastActiveAt = this[UsersTable.lastActiveAt],
        createdAt = this[UsersTable.createdAt],
        updatedAt = this[UsersTable.updatedAt]
    )
}