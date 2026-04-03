package io.github.mudrichenkoevgeny.backend.feature.user.database.repository.user

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.pagination.getNumOfTotalPages
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.listing.sorting.SortDirection
import io.github.mudrichenkoevgeny.backend.core.common.util.CollectionUtils.isAllArgsNull
import io.github.mudrichenkoevgeny.backend.core.database.extensions.applyPagination
import io.github.mudrichenkoevgeny.backend.feature.user.model.user.User
import io.github.mudrichenkoevgeny.backend.feature.user.model.user.UserListSort
import io.github.mudrichenkoevgeny.backend.feature.user.model.user.UserListSortBy
import io.github.mudrichenkoevgeny.backend.feature.user.database.table.UsersTable
import io.github.mudrichenkoevgeny.backend.core.common.model.UserId
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.UserRole
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
/**
 * Default [UserRepository] implementation backed by Exposed and [UsersTable].
 *
 * Performs synchronous Exposed DSL operations and maps [ResultRow] values into [User] models.
 * Returns [CommonError.Database] when inserts/updates report no affected rows.
 */
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
        accountStatus: UserAccountStatus?,
        sort: UserListSort = UserListSort.DEFAULT,
    ): AppResult<PagedResult<User>> {
        var query = UsersTable.selectAll()

        role?.let { r -> query = query.andWhere { UsersTable.role eq r } }
        accountStatus?.let { status -> query = query.andWhere { UsersTable.accountStatus eq status } }

        val totalCount = query.count().toLong()

        val (sortColumn, sortOrder) = sort.toExposedOrder()

        val users = query
            .orderBy(sortColumn to sortOrder, UsersTable.id to SortOrder.ASC)
            .applyPagination(params)
            .map { it.toUser() }

        return AppResult.Success(
            PagedResult(
                items = users,
                totalCount = totalCount,
                pageNumber = params.page,
                pageSize = params.size,
                totalPages = getNumOfTotalPages(totalCount, params.size),
            )
        )
    }

    private fun UserListSort.toExposedOrder(): Pair<Column<*>, SortOrder> {
        val order = when (this.order) {
            SortDirection.ASC -> SortOrder.ASC
            SortDirection.DESC -> SortOrder.DESC
        }
        val column: Column<*> = when (this.sortBy) {
            UserListSortBy.LAST_LOGIN_AT -> UsersTable.lastLoginAt
            UserListSortBy.LAST_ACTIVE_AT -> UsersTable.lastActiveAt
            UserListSortBy.CREATED_AT -> UsersTable.createdAt
            UserListSortBy.UPDATED_AT -> UsersTable.updatedAt
        }
        return column to order
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