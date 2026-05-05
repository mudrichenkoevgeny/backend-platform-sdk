package io.github.mudrichenkoevgeny.backend.feature.user.database.repository.user

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.model.UpdateField
import io.github.mudrichenkoevgeny.backend.core.common.model.onSet
import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.pagination.getNumOfTotalPages
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.mapNotNullOrError
import io.github.mudrichenkoevgeny.backend.core.common.util.toJavaInstant
import io.github.mudrichenkoevgeny.backend.core.common.util.toKotlinInstant
import io.github.mudrichenkoevgeny.backend.core.database.extensions.applyPagination
import io.github.mudrichenkoevgeny.backend.core.database.extensions.jsonbContainsAllStrings
import io.github.mudrichenkoevgeny.backend.core.database.mapper.toExposedSortOrder
import io.github.mudrichenkoevgeny.backend.feature.user.database.table.UsersTable
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.UserRoleAccessFilter
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
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
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.inList
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
            row[id] = user.id.value
            row[role] = user.role
            row[accountStatus] = user.accountStatus
            row[accountStatusBeforeDeletion] = user.accountStatusBeforeDeletion
            row[authorityLevel] = user.authorityLevel
            row[permissionCodes] = user.permissionCodes.map { it.value }.toSet()
            row[isTotpEnabled] = user.isTotpEnabled
            row[lastLoginAt] = user.lastLoginAt?.toJavaInstant()
            row[lastActiveAt] = user.lastActiveAt?.toJavaInstant()
            row[createdAt] = user.createdAt.toJavaInstant()
            row[updatedAt] = user.updatedAt?.toJavaInstant()
            row[scheduledPermanentDeletionAt] = user.scheduledPermanentDeletionAt?.toJavaInstant()
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
        userId: UserId,
        status: UpdateField<UserAccountStatus>,
        statusBeforeDeletion: UpdateField<UserAccountStatus>,
        authorityLevel: UpdateField<Int>,
        permissionCodes: UpdateField<Set<PermissionCode>>,
        isTotpEnabled: UpdateField<Boolean>,
        lastLoginAt: UpdateField<KotlinInstant>,
        lastActiveAt: UpdateField<KotlinInstant>,
        scheduledPermanentDeletionAt: UpdateField<KotlinInstant>
    ): AppResult<UserDetails> {
        val statusToSet = if (status is UpdateField.Set) {
            status.value
                ?: return AppResult.Error(CommonError.Database("Status cannot be null"))
        } else null

        val authorityLevelToSet = if (authorityLevel is UpdateField.Set) {
            authorityLevel.value
                ?: return AppResult.Error(CommonError.Database("Authority level cannot be null"))
        } else null

        val isTotpEnabledToSet = if (isTotpEnabled is UpdateField.Set) {
            isTotpEnabled.value
                ?: return AppResult.Error(CommonError.Database("isTotpEnabled cannot be null"))
        } else null

        val updatedAtJavaInstant = JavaInstant.now()

        val updatedRows = UsersTable.update({ UsersTable.id eq userId.value }) { updateStatement ->
            statusToSet?.let { accountStatus ->
                updateStatement[UsersTable.accountStatus] = accountStatus
            }

            statusBeforeDeletion.onSet { accountStatusBeforeDeletion ->
                updateStatement[UsersTable.accountStatusBeforeDeletion] = accountStatusBeforeDeletion
            }

            authorityLevelToSet?.let { level ->
                updateStatement[UsersTable.authorityLevel] = level
            }

            permissionCodes.onSet { codes ->
                updateStatement[UsersTable.permissionCodes] = codes?.map { permissionCode ->
                    permissionCode.value
                }?.toSet() ?: emptySet()
            }

            isTotpEnabledToSet?.let { enabled ->
                updateStatement[UsersTable.isTotpEnabled] = enabled
            }

            lastLoginAt.onSet { lastLoginAt ->
                updateStatement[UsersTable.lastLoginAt] = lastLoginAt?.toJavaInstant()
            }

            lastActiveAt.onSet { lastActiveAt ->
                updateStatement[UsersTable.lastActiveAt] = lastActiveAt?.toJavaInstant()
            }

            scheduledPermanentDeletionAt.onSet { scheduledDeletionAt ->
                updateStatement[UsersTable.scheduledPermanentDeletionAt] = scheduledDeletionAt?.toJavaInstant()
            }

            updateStatement[UsersTable.updatedAt] = updatedAtJavaInstant
        }

        if (updatedRows == 0) {
            return AppResult.Error(
                CommonError.Database("Failed to update fields for user id=${userId.value}")
            )
        }

        return getUserDetailsById(userId).mapNotNullOrError(UserError.UserNotFound(userId))
    }

    override suspend fun getUserDetailsById(userId: UserId): AppResult<UserDetails?> {
        return AppResult.Success(getUserResultRow(userId)?.toUserDetails())
    }

    private fun getUserResultRow(userId: UserId): ResultRow? {
        return UsersTable
            .selectAll()
            .where { UsersTable.id eq userId.value }
            .singleOrNull()
    }

    override suspend fun getUsersPageWithAccessFilter(
        accessFilter: UserRoleAccessFilter,
        pageParams: PageParams,
        sortBy: UserSortValues.UserSortBy,
        sortOrder: SortOrder,
        roles: List<UserRole>,
        accountStatuses: List<UserAccountStatus>,
        accountStatusesBeforeDeletion: List<UserAccountStatus>,
        authorityLevelFrom: Int?,
        authorityLevelTo: Int?,
        permissionCodes: Set<PermissionCode>,
        isTotpEnabled: Boolean?
    ): AppResult<PagedResult<UserDetails>> {
        val query = UsersTable.selectAll()

        query.andWhere {
            val roleConditions = accessFilter.allowedUserRoles
                .map { allowedRole -> UsersTable.role eq allowedRole }
            roleConditions.reduceOrNull { acc, op -> acc or op } ?: Op.FALSE
        }

        if (roles.isNotEmpty()) {
            query.andWhere { UsersTable.role inList roles }
        }
        if (accountStatuses.isNotEmpty()) {
            query.andWhere { UsersTable.accountStatus inList accountStatuses }
        }
        if (accountStatusesBeforeDeletion.isNotEmpty()) {
            query.andWhere { UsersTable.accountStatusBeforeDeletion inList accountStatusesBeforeDeletion }
        }

        if (authorityLevelFrom != null) {
            query.andWhere { UsersTable.authorityLevel greaterEq authorityLevelFrom }
        }
        if (authorityLevelTo != null) {
            query.andWhere { UsersTable.authorityLevel lessEq authorityLevelTo }
        }

        if (isTotpEnabled != null) {
            query.andWhere { UsersTable.isTotpEnabled eq isTotpEnabled }
        }

        if (permissionCodes.isNotEmpty()) {
            query.andWhere {
                UsersTable.permissionCodes jsonbContainsAllStrings permissionCodes.map { it.value }.toSet()
            }
        }

        val totalCount = query.count()

        val sortColumn = when (sortBy) {
            UserSortValues.UserSortBy.LAST_LOGIN_AT -> UsersTable.lastLoginAt
            UserSortValues.UserSortBy.LAST_ACTIVE_AT -> UsersTable.lastActiveAt
            UserSortValues.UserSortBy.SCHEDULED_PERMANENT_DELETION_AT -> UsersTable.scheduledPermanentDeletionAt
            UserSortValues.UserSortBy.CREATED_AT -> UsersTable.createdAt
            UserSortValues.UserSortBy.UPDATED_AT -> UsersTable.updatedAt
        }

        val users = query
            .orderBy(sortColumn to sortOrder.toExposedSortOrder())
            .applyPagination(pageParams)
            .map { it.toUserDetails() }

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

    private fun ResultRow.toUserDetails(): UserDetails {
        return UserDetails(
            id = UserId(this[UsersTable.id].value),
            role = this[UsersTable.role],
            accountStatus = this[UsersTable.accountStatus],
            accountStatusBeforeDeletion = this[UsersTable.accountStatusBeforeDeletion],
            authorityLevel = this[UsersTable.authorityLevel],
            permissionCodes = this[UsersTable.permissionCodes].map { PermissionCode(it) }.toSet(),
            isTotpEnabled = this[UsersTable.isTotpEnabled],
            lastLoginAt = this[UsersTable.lastLoginAt]?.toKotlinInstant(),
            lastActiveAt = this[UsersTable.lastActiveAt]?.toKotlinInstant(),
            createdAt = this[UsersTable.createdAt].toKotlinInstant(),
            updatedAt = this[UsersTable.updatedAt]?.toKotlinInstant(),
            scheduledPermanentDeletionAt = this[UsersTable.scheduledPermanentDeletionAt]?.toKotlinInstant()
        )
    }
}
