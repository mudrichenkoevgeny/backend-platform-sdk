package io.github.mudrichenkoevgeny.backend.feature.user.database.repository.user

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.pagination.getNumOfTotalPages
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.util.toJavaInstant
import io.github.mudrichenkoevgeny.backend.core.common.util.toKotlinInstant
import io.github.mudrichenkoevgeny.backend.core.database.extensions.applyPagination
import io.github.mudrichenkoevgeny.backend.core.database.extensions.jsonbContainsSingleString
import io.github.mudrichenkoevgeny.backend.core.database.mapper.toExposedSortOrder
import io.github.mudrichenkoevgeny.backend.feature.user.database.table.UsersTable
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.UserRoleAccessFilter
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.SortOrder
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.permission.PermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserSortValues
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserDetails
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Instant as JavaInstant
import javax.inject.Inject
import kotlin.time.Instant as KotlinInstant
import javax.inject.Singleton

/**
 * Default [UserRepository] implementation backed by Exposed and [UsersTable].
 *
 * Performs synchronous Exposed DSL operations and maps [ResultRow] values into [UserDetails].
 * Returns [CommonError.Database] when inserts/updates report no affected rows.
 */
@Singleton
class UserRepositoryImpl @Inject constructor() : UserRepository {

    override suspend fun createUser(user: UserDetails): AppResult<UserDetails> {
        val inserted = UsersTable.insert { row ->
            row[UsersTable.id] = user.id.value
            row[UsersTable.role] = user.role
            row[UsersTable.accountStatus] = user.accountStatus
            row[UsersTable.accountStatusBeforeDeletion] = user.accountStatusBeforeDeletion
            row[UsersTable.permissions] = user.permissions.map { userPermissionCode ->
                userPermissionCode.value
            }.toSet()
            row[UsersTable.lastLoginAt] = user.lastLoginAt?.toJavaInstant()
            row[UsersTable.lastActiveAt] = user.lastActiveAt?.toJavaInstant()
            row[UsersTable.createdAt] = user.createdAt.toJavaInstant()
            row[UsersTable.updatedAt] = user.updatedAt?.toJavaInstant()
            row[UsersTable.scheduledPermanentDeletionAt] = user.scheduledPermanentDeletionAt?.toJavaInstant()
        }

        if (inserted.insertedCount == 0) {
            return AppResult.Error(
                CommonError.Database("User creation failed for id=${user.id.value}")
            )
        }

        return AppResult.Success(user)
    }

    override suspend fun deleteUser(userId: UserId): AppResult<Unit> {
        UsersTable.deleteWhere { UsersTable.id eq userId.value }
        return AppResult.Success(Unit)
    }

    override suspend fun updateUser(
        user: UserDetails,
        status: UserAccountStatus?,
        statusBeforeDeletion: UserAccountStatus?,
        permissions: Set<PermissionCode>,
        lastLoginAt: KotlinInstant?,
        lastActiveAt: KotlinInstant?,
        scheduledPermanentDeletionAt: KotlinInstant?
    ): AppResult<UserDetails> {
        val permissionsUpdated = permissions.isNotEmpty()
        if (
            status == null &&
            statusBeforeDeletion == null &&
            !permissionsUpdated &&
            lastLoginAt == null &&
            lastActiveAt == null &&
            scheduledPermanentDeletionAt == null
        ) {
            return AppResult.Success(user)
        }

        val updatedAtJavaInstant = JavaInstant.now()

        val updatedRows = UsersTable.update({ UsersTable.id eq user.id.value }) { stmt ->
            if (status != null) {
                stmt[UsersTable.accountStatus] = status
            }
            if (statusBeforeDeletion != null) {
                stmt[UsersTable.accountStatusBeforeDeletion] = statusBeforeDeletion
            }
            if (permissionsUpdated) {
                stmt[UsersTable.permissions] = permissions.map { it.value }.toSet()
            }
            if (lastLoginAt != null) {
                stmt[UsersTable.lastLoginAt] = lastLoginAt.toJavaInstant()
            }
            if (lastActiveAt != null) {
                stmt[UsersTable.lastActiveAt] = lastActiveAt.toJavaInstant()
            }
            if (scheduledPermanentDeletionAt != null) {
                stmt[UsersTable.scheduledPermanentDeletionAt] = scheduledPermanentDeletionAt.toJavaInstant()
            }
            stmt[UsersTable.updatedAt] = updatedAtJavaInstant
        }

        if (updatedRows == 0) {
            return AppResult.Error(
                CommonError.Database("Failed to update fields for user id=${user.id.value}")
            )
        }

        val updatedRow = UsersTable
            .selectAll()
            .where { UsersTable.id eq user.id.value }
            .singleOrNull()
            ?: return AppResult.Error(CommonError.Database("Updated user not found for id=${user.id.value}"))

        return AppResult.Success(updatedRow.toUser())
    }

    override suspend fun getUserById(userId: UserId): AppResult<UserDetails?> {
        val resultRow = UsersTable
            .selectAll()
            .where { UsersTable.id eq userId.value }
            .singleOrNull()

        return AppResult.Success(resultRow?.toUser())
    }

    override suspend fun getUsersList(
        accessFilter: UserRoleAccessFilter,
        pageParams: PageParams,
        sortBy: UserSortValues.UserSortBy,
        sortOrder: SortOrder,
        role: UserRole?,
        accountStatus: UserAccountStatus?,
        accountStatusBeforeDeletion: UserAccountStatus?,
        userPermissionCode: PermissionCode?
    ): AppResult<PagedResult<UserDetails>> {
        var query = UsersTable.selectAll()

        query = query.andWhere {
            val roleConditions = accessFilter.allowedUserRoles
                .map { allowedRole -> UsersTable.role eq allowedRole }

            roleConditions.reduceOrNull { acc, op -> acc or op } ?: Op.FALSE
        }

        role?.let { r -> query = query.andWhere { UsersTable.role eq r } }
        accountStatus?.let { status -> query = query.andWhere { UsersTable.accountStatus eq status } }
        accountStatusBeforeDeletion?.let { status ->
            query = query.andWhere { UsersTable.accountStatusBeforeDeletion eq status }
        }
        userPermissionCode?.let { code ->
            query = query.andWhere { UsersTable.permissions jsonbContainsSingleString code.value }
        }

        val totalCount = query.count()

        val sortColumn = when (sortBy) {
            UserSortValues.UserSortBy.LAST_LOGIN_AT -> UsersTable.lastLoginAt
            UserSortValues.UserSortBy.LAST_ACTIVE_AT -> UsersTable.lastActiveAt
            UserSortValues.UserSortBy.SCHEDULED_PERMANENT_DELETION_AT -> UsersTable.scheduledPermanentDeletionAt
            UserSortValues.UserSortBy.CREATED_AT -> UsersTable.createdAt
            UserSortValues.UserSortBy.UPDATED_AT -> UsersTable.updatedAt
        }
        val exposedSortOrder = sortOrder.toExposedSortOrder()

        val users = query
            .orderBy(sortColumn to exposedSortOrder)
            .applyPagination(pageParams)
            .map { it.toUser() }

        val totalPages = getNumOfTotalPages(totalCount, pageParams.size)

        return AppResult.Success(
            PagedResult(
                items = users,
                totalCount = totalCount,
                pageNumber = pageParams.page,
                pageSize = pageParams.size,
                totalPages = totalPages
            )
        )
    }

    override suspend fun deleteUsersDueForPermanentDeletion(asOf: KotlinInstant): AppResult<Int> {
        val asOfJavaInstant = asOf.toJavaInstant()
        val deletedCount = UsersTable.deleteWhere {
            (UsersTable.scheduledPermanentDeletionAt.isNotNull()) and
                (UsersTable.scheduledPermanentDeletionAt lessEq asOfJavaInstant)
        }
        return AppResult.Success(deletedCount)
    }

    private fun ResultRow.toUser(): UserDetails {
        return UserDetails(
            id = UserId(this[UsersTable.id].value),
            role = this[UsersTable.role],
            accountStatus = this[UsersTable.accountStatus],
            accountStatusBeforeDeletion = this[UsersTable.accountStatusBeforeDeletion],
            permissions = this[UsersTable.permissions].mapNotNull { permissionString ->
                permissionString
                    .takeIf { it.isNotBlank() }
                    ?.let { PermissionCode(it) }
            }.toSet(),
            lastLoginAt = this[UsersTable.lastLoginAt]?.toKotlinInstant(),
            lastActiveAt = this[UsersTable.lastActiveAt]?.toKotlinInstant(),
            createdAt = this[UsersTable.createdAt].toKotlinInstant(),
            updatedAt = this[UsersTable.updatedAt]?.toKotlinInstant(),
            scheduledPermanentDeletionAt = this[UsersTable.scheduledPermanentDeletionAt]?.toKotlinInstant()
        )
    }
}
