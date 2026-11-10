package io.github.mudrichenkoevgeny.backend.feature.user.database.repository.user

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.model.UpdateField
import io.github.mudrichenkoevgeny.backend.core.common.model.onSet
import io.github.mudrichenkoevgeny.backend.core.common.pagination.getNumOfTotalPages
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.mapNotNullOrError
import io.github.mudrichenkoevgeny.backend.core.common.util.mapToSet
import io.github.mudrichenkoevgeny.backend.core.common.util.toJavaInstant
import io.github.mudrichenkoevgeny.backend.core.common.util.toKotlinInstant
import io.github.mudrichenkoevgeny.backend.core.database.dsl.applyNullabilityFilter
import io.github.mudrichenkoevgeny.backend.core.database.dsl.applyPagination
import io.github.mudrichenkoevgeny.backend.core.database.dsl.applyTimestampFilter
import io.github.mudrichenkoevgeny.backend.core.database.dsl.jsonbContainsAllStrings
import io.github.mudrichenkoevgeny.backend.core.database.dsl.substringSqlLikePattern
import io.github.mudrichenkoevgeny.backend.core.database.error.PostgreSqlErrorCodes
import io.github.mudrichenkoevgeny.backend.core.database.mapper.toExposedSortOrder
import io.github.mudrichenkoevgeny.backend.feature.user.database.table.UsersTable
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.UserRoleAccessFilter
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.user.UpdateUserParams
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.network.model.UserListQueryParams
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.permission.PermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.accountlockout.AccountLockoutType
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.file.FileId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserSortBy
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserInternal
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserStorageConfiguration
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.not
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
import java.time.Instant as JavaInstant
import kotlin.time.Instant as KotlinInstant

/**
 * Default [UserRepository] implementation backed by Exposed and [UsersTable].
 *
 * Performs synchronous Exposed DSL operations and maps [ResultRow] values into [UserInternal].
 * Returns [CommonError.Database] when inserts/updates report no affected rows.
 */
@Singleton
public class UserRepositoryImpl @Inject constructor() : UserRepository {
    override suspend fun createUser(user: UserInternal): AppResult<UserInternal> {
        try {
            val inserted = UsersTable.insert { row ->
                row[id] = user.id.value
                row[role] = user.role
                row[accountStatus] = user.accountStatus
                row[accountStatusOnRestore] = user.accountStatusOnRestore
                row[authorityLevel] = user.authorityLevel
                row[permissionCodes] = user.permissionCodes.mapToSet { permissionCode ->
                    permissionCode.value
                }
                row[isTotpEnabled] = user.isTotpEnabled
                row[storageQuotaBytes] = user.storageConfiguration?.quotaBytes
                row[storageMaxUploadSizeBytes] = user.storageConfiguration?.maxUploadSizeBytes
                row[storageMaxPendingUploads] = user.storageConfiguration?.maxPendingUploads
                row[lastLoginAt] = user.lastLoginAt?.toJavaInstant()
                row[lastActiveAt] = user.lastActiveAt?.toJavaInstant()
                row[createdAt] = user.createdAt.toJavaInstant()
                row[updatedAt] = user.updatedAt?.toJavaInstant()
                row[scheduledPermanentDeletionAt] = user.scheduledPermanentDeletionAt?.toJavaInstant()
                row[accountLockoutType] = user.lockoutType
                row[temporaryLockoutUntil] = user.temporaryLockoutUntil?.toJavaInstant()
                row[nickname] = user.nickname
                row[displayName] = user.displayName
                row[avatarFileId] = user.avatarFileId?.value
                row[birthdate] = user.birthdate?.toJavaInstant()
            }

            if (inserted.insertedCount == 0) {
                return AppResult.Error(
                    CommonError.Database("User creation failed for id=${user.id.value}")
                )
            }

            return AppResult.Success(user)
        } catch (e: ExposedSQLException) {
            val isUniqueViolation = e.sqlState == PostgreSqlErrorCodes.UNIQUE_VIOLATION
            val isNicknameConstraint = e.message?.contains(
                UsersTable.NICKNAME_UNIQUE_INDEX_NAME,
                ignoreCase = true
            ) == true

            if (isUniqueViolation && isNicknameConstraint) {
                val nick = user.nickname ?: ""
                return AppResult.Error(UserError.NicknameAlreadyTaken(nick))
            }
            return AppResult.Error(
                CommonError.Database("User creation failed for id=${user.id.value}: ${e.message}")
            )
        }
    }

    override suspend fun deleteUser(userId: UserId): AppResult<Unit> {
        UsersTable.deleteWhere { UsersTable.id eq userId.value }
        return AppResult.Success(Unit)
    }

    override suspend fun updateUser(
        userId: UserId,
        updateUserParams: UpdateUserParams
    ): AppResult<UserInternal> {
        val accountStatusToSet = if (updateUserParams.accountStatus is UpdateField.Set) {
            updateUserParams.accountStatus.value ?: return AppResult.Error(
                CommonError.Database("Status cannot be null")
            )
        } else {
            null
        }

        val authorityLevelToSet = if (updateUserParams.authorityLevel is UpdateField.Set) {
            updateUserParams.authorityLevel.value ?: return AppResult.Error(
                CommonError.Database("Authority level cannot be null")
            )
        } else {
            null
        }

        val isTotpEnabledToSet = if (updateUserParams.isTotpEnabled is UpdateField.Set) {
            updateUserParams.isTotpEnabled.value ?: return AppResult.Error(
                CommonError.Database("isTotpEnabled cannot be null")
            )
        } else {
            null
        }

        val updatedAtJavaInstant = JavaInstant.now()

        try {
            val updatedRows = UsersTable.update({ UsersTable.id eq userId.value }) { updateStatement ->
                accountStatusToSet?.let { statusToSet ->
                    updateStatement[UsersTable.accountStatus] = statusToSet
                }

                updateUserParams.accountStatusOnRestore.onSet { statusOnRestore ->
                    updateStatement[UsersTable.accountStatusOnRestore] = statusOnRestore
                }

                authorityLevelToSet?.let { authorityLevel ->
                    updateStatement[UsersTable.authorityLevel] = authorityLevel
                }

                updateUserParams.permissions.onSet { permissionCodes ->
                    updateStatement[UsersTable.permissionCodes] = permissionCodes?.map { permissionCode ->
                        permissionCode.value
                    }?.toSet() ?: emptySet()
                }

                isTotpEnabledToSet?.let { isTotpEnabled ->
                    updateStatement[UsersTable.isTotpEnabled] = isTotpEnabled
                }

                updateUserParams.storageConfiguration.onSet { config ->
                    updateStatement[UsersTable.storageQuotaBytes] = config?.quotaBytes
                    updateStatement[UsersTable.storageMaxUploadSizeBytes] = config?.maxUploadSizeBytes
                    updateStatement[UsersTable.storageMaxPendingUploads] = config?.maxPendingUploads
                }

                updateUserParams.scheduledPermanentDeletionAt.onSet { scheduledPermanentDeletionAt ->
                    updateStatement[UsersTable.scheduledPermanentDeletionAt] =
                        scheduledPermanentDeletionAt?.toJavaInstant()
                }

                updateUserParams.lockoutType.onSet { accountLockoutType ->
                    if (accountLockoutType != null) {
                        updateStatement[UsersTable.accountLockoutType] = accountLockoutType
                    }
                }

                updateUserParams.temporaryLockoutUntil.onSet { temporaryLockoutUntil ->
                    updateStatement[UsersTable.temporaryLockoutUntil] = temporaryLockoutUntil?.toJavaInstant()
                }

                updateUserParams.nickname.onSet { nickname ->
                    updateStatement[UsersTable.nickname] = nickname
                }

                updateUserParams.displayName.onSet { displayName ->
                    updateStatement[UsersTable.displayName] = displayName
                }

                updateUserParams.avatarFileId.onSet { avatarFileId ->
                    updateStatement[UsersTable.avatarFileId] = avatarFileId?.value
                }

                updateUserParams.birthdate.onSet { birthdate ->
                    updateStatement[UsersTable.birthdate] = birthdate?.toJavaInstant()
                }

                updateStatement[UsersTable.updatedAt] = updatedAtJavaInstant
            }

            if (updatedRows == 0) {
                return AppResult.Error(
                    CommonError.Database("Failed to update fields for user id=${userId.value}")
                )
            }
        } catch (e: ExposedSQLException) {
            val isUniqueViolation = e.sqlState == PostgreSqlErrorCodes.UNIQUE_VIOLATION
            val isNicknameConstraint = e.message?.contains(
                UsersTable.NICKNAME_UNIQUE_INDEX_NAME,
                ignoreCase = true
            ) == true

            if (isUniqueViolation && isNicknameConstraint) {
                val newNick = (updateUserParams.nickname as? UpdateField.Set)?.value ?: ""
                return AppResult.Error(UserError.NicknameAlreadyTaken(newNick))
            }
            return AppResult.Error(
                CommonError.Database("Failed to update fields for user id=${userId.value}: ${e.message}")
            )
        }

        return getUserInternalById(userId).mapNotNullOrError(UserError.UserNotFound(userId))
    }

    override suspend fun getUserInternalById(userId: UserId): AppResult<UserInternal?> = try {
        AppResult.Success(getUserResultRow(userId)?.toUserInternal())
    } catch (e: ExposedSQLException) {
        AppResult.Error(CommonError.Database("Error retrieving user by id: ${e.message}"))
    }

    override suspend fun getUserInternalByNickname(nickname: String): AppResult<UserInternal?> = try {
        val resultRow = UsersTable.selectAll().where { UsersTable.nickname.lowerCase() eq nickname.lowercase() }
            .singleOrNull()

        AppResult.Success(resultRow?.toUserInternal())
    } catch (e: ExposedSQLException) {
        AppResult.Error(CommonError.Database("Error retrieving user by nickname: ${e.message}"))
    }

    private fun getUserResultRow(userId: UserId): ResultRow? =
        UsersTable.selectAll().where { UsersTable.id eq userId.value }.singleOrNull()

    override suspend fun getUsersPageWithAccessFilter(
        accessFilter: UserRoleAccessFilter,
        queryParams: UserListQueryParams
    ): AppResult<PagedResult<UserInternal>> = try {
        val query = UsersTable.selectAll()

        applyRoleAccessFilter(query, accessFilter)
        applyIdentityFilters(query, queryParams)
        applyAccountStatusFilters(query, queryParams)
        applyAuthorityFilters(query, queryParams)
        applyLockoutFilters(query, queryParams)
        applyProfileFilters(query, queryParams)
        applyTimestampFilters(query, queryParams)
        applyStorageFilters(query, queryParams)
        applySearchFilter(query, queryParams)
        applyPermissionFilters(query, queryParams)

        val totalCount = query.count()

        val sortColumn = when (queryParams.listing.sortBy) {
            UserSortBy.AUTHORITY_LEVEL -> UsersTable.authorityLevel
            UserSortBy.LAST_LOGIN_AT -> UsersTable.lastLoginAt
            UserSortBy.LAST_ACTIVE_AT -> UsersTable.lastActiveAt
            UserSortBy.SCHEDULED_PERMANENT_DELETION_AT -> UsersTable.scheduledPermanentDeletionAt
            UserSortBy.TEMPORARY_LOCKOUT_UNTIL -> UsersTable.temporaryLockoutUntil
            UserSortBy.NICKNAME -> UsersTable.nickname
            UserSortBy.DISPLAY_NAME -> UsersTable.displayName
            UserSortBy.BIRTHDATE -> UsersTable.birthdate
            UserSortBy.CREATED_AT -> UsersTable.createdAt
            UserSortBy.UPDATED_AT -> UsersTable.updatedAt
        }

        val users = query.orderBy(sortColumn to queryParams.listing.sortOrder.toExposedSortOrder())
            .applyPagination(queryParams.listing.pageParams).map { it.toUserInternal() }

        val totalPages = getNumOfTotalPages(totalCount, queryParams.listing.pageParams.size)

        AppResult.Success(
            PagedResult(
                items = users,
                totalCount = totalCount,
                pageNumber = queryParams.listing.pageParams.page,
                pageSize = queryParams.listing.pageParams.size,
                totalPages = totalPages
            )
        )
    } catch (e: ExposedSQLException) {
        AppResult.Error(CommonError.Database("Error retrieving users page: ${e.message}"))
    }

    private fun applyRoleAccessFilter(
        query: Query,
        accessFilter: UserRoleAccessFilter
    ) {
        query.andWhere {
            val roleConditions = accessFilter.allowedUserRoles.map { allowedRole -> UsersTable.role eq allowedRole }
            roleConditions.reduceOrNull { acc, op -> acc or op } ?: Op.FALSE
        }
    }

    private fun applyIdentityFilters(
        query: Query,
        queryParams: UserListQueryParams
    ) {
        if (queryParams.userIds.isNotEmpty()) {
            val entityIds = queryParams.userIds.mapNotNull { userIdString ->
                runCatching { EntityID(Uuid.parse(userIdString), UsersTable) }.getOrNull()
            }
            if (entityIds.isNotEmpty()) {
                query.andWhere { UsersTable.id inList entityIds }
            }
        }
        if (queryParams.roles.isNotEmpty()) {
            query.andWhere { UsersTable.role inList queryParams.roles }
        }
        if (queryParams.excludeRoles.isNotEmpty()) {
            query.andWhere { UsersTable.role notInList queryParams.excludeRoles }
        }
    }

    private fun applyAccountStatusFilters(
        query: Query,
        queryParams: UserListQueryParams
    ) {
        if (queryParams.accountStatuses.isNotEmpty()) {
            query.andWhere { UsersTable.accountStatus inList queryParams.accountStatuses }
        }
        if (queryParams.excludeAccountStatuses.isNotEmpty()) {
            query.andWhere { UsersTable.accountStatus notInList queryParams.excludeAccountStatuses }
        }
        if (queryParams.accountStatusesOnRestore.isNotEmpty()) {
            query.andWhere { UsersTable.accountStatusOnRestore inList queryParams.accountStatusesOnRestore }
        }
        if (queryParams.excludeAccountStatusesOnRestore.isNotEmpty()) {
            query.andWhere {
                UsersTable.accountStatusOnRestore notInList queryParams.excludeAccountStatusesOnRestore
            }
        }
        query.applyNullabilityFilter(UsersTable.accountStatusOnRestore, queryParams.isAccountStatusOnRestoreSet)
    }

    private fun applyAuthorityFilters(
        query: Query,
        queryParams: UserListQueryParams
    ) {
        if (queryParams.authorityLevelFrom != null) {
            query.andWhere { UsersTable.authorityLevel greaterEq queryParams.authorityLevelFrom }
        }
        if (queryParams.authorityLevelTo != null) {
            query.andWhere { UsersTable.authorityLevel lessEq queryParams.authorityLevelTo }
        }
        if (queryParams.isTotpEnabled != null) {
            query.andWhere { UsersTable.isTotpEnabled eq queryParams.isTotpEnabled }
        }
    }

    private fun applyLockoutFilters(
        query: Query,
        queryParams: UserListQueryParams
    ) {
        if (queryParams.accountLockoutTypes.isNotEmpty()) {
            query.andWhere { UsersTable.accountLockoutType inList queryParams.accountLockoutTypes }
        }
        if (queryParams.excludeAccountLockoutTypes.isNotEmpty()) {
            query.andWhere { UsersTable.accountLockoutType notInList queryParams.excludeAccountLockoutTypes }
        }
    }

    private fun applyProfileFilters(
        query: Query,
        queryParams: UserListQueryParams
    ) {
        if (!queryParams.nickname.isNullOrBlank()) {
            query.andWhere { UsersTable.nickname.lowerCase() eq queryParams.nickname.lowercase() }
        }
        query.applyNullabilityFilter(UsersTable.nickname, queryParams.isNicknameSet)

        if (!queryParams.displayName.isNullOrBlank()) {
            query.andWhere { UsersTable.displayName.lowerCase() eq queryParams.displayName.lowercase() }
        }
        query.applyNullabilityFilter(UsersTable.displayName, queryParams.isDisplayNameSet)

        query.applyNullabilityFilter(UsersTable.avatarFileId, queryParams.isAvatarSet)
    }

    private fun applyTimestampFilters(
        query: Query,
        queryParams: UserListQueryParams
    ) {
        query.applyTimestampFilter(
            column = UsersTable.createdAt,
            fromInstant = queryParams.createdAtFrom,
            toInstant = queryParams.createdAtTo
        )
        query.applyTimestampFilter(
            column = UsersTable.updatedAt,
            fromInstant = queryParams.updatedAtFrom,
            toInstant = queryParams.updatedAtTo,
            isSet = queryParams.isUpdatedAtSet
        )
        query.applyTimestampFilter(
            column = UsersTable.lastLoginAt,
            fromInstant = queryParams.lastLoginAtFrom,
            toInstant = queryParams.lastLoginAtTo,
            isSet = queryParams.isLastLoginAtSet
        )
        query.applyTimestampFilter(
            column = UsersTable.lastActiveAt,
            fromInstant = queryParams.lastActiveAtFrom,
            toInstant = queryParams.lastActiveAtTo,
            isSet = queryParams.isLastActiveAtSet
        )
        query.applyTimestampFilter(
            column = UsersTable.scheduledPermanentDeletionAt,
            fromInstant = queryParams.scheduledPermanentDeletionAtFrom,
            toInstant = queryParams.scheduledPermanentDeletionAtTo,
            isSet = queryParams.isScheduledPermanentDeletionAtSet
        )
        query.applyTimestampFilter(
            column = UsersTable.temporaryLockoutUntil,
            fromInstant = queryParams.temporaryLockoutUntilFrom,
            toInstant = queryParams.temporaryLockoutUntilTo,
            isSet = queryParams.isTemporaryLockoutUntilSet
        )
        query.applyTimestampFilter(
            column = UsersTable.birthdate,
            fromInstant = queryParams.birthdateFrom,
            toInstant = queryParams.birthdateTo,
            isSet = queryParams.isBirthdateSet
        )
    }

    private fun applyStorageFilters(
        query: Query,
        queryParams: UserListQueryParams
    ) {
        if (queryParams.storageQuotaBytesFrom != null) {
            query.andWhere { UsersTable.storageQuotaBytes greaterEq queryParams.storageQuotaBytesFrom }
        }
        if (queryParams.storageQuotaBytesTo != null) {
            query.andWhere { UsersTable.storageQuotaBytes lessEq queryParams.storageQuotaBytesTo }
        }
        if (queryParams.isStorageQuotaBytesSet == true) {
            query.andWhere { UsersTable.storageQuotaBytes.isNotNull() }
        } else if (queryParams.isStorageQuotaBytesSet == false) {
            query.andWhere { UsersTable.storageQuotaBytes.isNull() }
        }

        if (queryParams.storageMaxUploadSizeBytesFrom != null) {
            query.andWhere {
                UsersTable.storageMaxUploadSizeBytes greaterEq queryParams.storageMaxUploadSizeBytesFrom
            }
        }
        if (queryParams.storageMaxUploadSizeBytesTo != null) {
            query.andWhere {
                UsersTable.storageMaxUploadSizeBytes lessEq queryParams.storageMaxUploadSizeBytesTo
            }
        }
        if (queryParams.isStorageMaxUploadSizeBytesSet == true) {
            query.andWhere { UsersTable.storageMaxUploadSizeBytes.isNotNull() }
        } else if (queryParams.isStorageMaxUploadSizeBytesSet == false) {
            query.andWhere { UsersTable.storageMaxUploadSizeBytes.isNull() }
        }

        if (queryParams.storageMaxPendingUploadsFrom != null) {
            query.andWhere {
                UsersTable.storageMaxPendingUploads greaterEq queryParams.storageMaxPendingUploadsFrom
            }
        }
        if (queryParams.storageMaxPendingUploadsTo != null) {
            query.andWhere { UsersTable.storageMaxPendingUploads lessEq queryParams.storageMaxPendingUploadsTo }
        }
        query.applyNullabilityFilter(
            UsersTable.storageMaxPendingUploads,
            queryParams.isStorageMaxPendingUploadsSet
        )

        if (queryParams.isStorageConfigurationSet == true) {
            query.andWhere {
                UsersTable.storageQuotaBytes.isNotNull() or UsersTable.storageMaxUploadSizeBytes.isNotNull() or
                    UsersTable.storageMaxPendingUploads.isNotNull()
            }
        } else if (queryParams.isStorageConfigurationSet == false) {
            query.andWhere {
                UsersTable.storageQuotaBytes.isNull() and UsersTable.storageMaxUploadSizeBytes.isNull() and
                    UsersTable.storageMaxPendingUploads.isNull()
            }
        }
    }

    private fun applySearchFilter(
        query: Query,
        queryParams: UserListQueryParams
    ) {
        if (!queryParams.searchQuery.isNullOrBlank()) {
            val pattern = substringSqlLikePattern(queryParams.searchQuery.lowercase())
            query.andWhere {
                (UsersTable.nickname.lowerCase() like pattern) or (UsersTable.displayName.lowerCase() like pattern)
            }
        }
    }

    private fun applyPermissionFilters(
        query: Query,
        queryParams: UserListQueryParams
    ) {
        if (queryParams.requiredPermissionCodes.isNotEmpty()) {
            query.andWhere {
                UsersTable.permissionCodes jsonbContainsAllStrings
                    queryParams.requiredPermissionCodes.mapToSet { it.value }
            }
        }
        if (queryParams.excludePermissionCodes.isNotEmpty()) {
            query.andWhere {
                not(
                    UsersTable.permissionCodes jsonbContainsAllStrings
                        queryParams.excludePermissionCodes.mapToSet { it.value }
                )
            }
        }
    }

    override suspend fun deleteUsersDueForPermanentDeletion(asOf: KotlinInstant): AppResult<Int> = try {
        val asOfJavaInstant = asOf.toJavaInstant()
        val deletedCount = UsersTable.deleteWhere {
            UsersTable.scheduledPermanentDeletionAt lessEq asOfJavaInstant
        }
        AppResult.Success(deletedCount)
    } catch (e: ExposedSQLException) {
        AppResult.Error(CommonError.Database("Error deleting users due for deletion: ${e.message}"))
    }

    override suspend fun getExpiredTemporaryLockouts(asOf: KotlinInstant): AppResult<List<UserId>> = try {
        val asOfJavaInstant = asOf.toJavaInstant()
        val userIds = UsersTable.selectAll().where {
            (UsersTable.accountLockoutType eq AccountLockoutType.TEMPORARY) and
                (UsersTable.temporaryLockoutUntil lessEq asOfJavaInstant)
        }.map { UserId(it[UsersTable.id].value) }
        AppResult.Success(userIds)
    } catch (e: ExposedSQLException) {
        AppResult.Error(CommonError.Database("Error retrieving expired temporary lockouts: ${e.message}"))
    }

    override suspend fun unlockUsersBatch(userIds: List<UserId>): AppResult<Unit> {
        if (userIds.isEmpty()) {
            return AppResult.Success(Unit)
        }
        return try {
            val ids = userIds.map { it.value }
            UsersTable.update({ UsersTable.id inList ids }) { row ->
                row[UsersTable.accountLockoutType] = AccountLockoutType.NONE
                row[UsersTable.temporaryLockoutUntil] = null
                row[UsersTable.updatedAt] = JavaInstant.now()
            }
            AppResult.Success(Unit)
        } catch (e: ExposedSQLException) {
            AppResult.Error(CommonError.Database("Error unlocking users batch: ${e.message}"))
        }
    }

    private fun ResultRow.toUserInternal(): UserInternal {
        val quotaBytes = this[UsersTable.storageQuotaBytes]
        val maxUploadSizeBytes = this[UsersTable.storageMaxUploadSizeBytes]
        val maxPendingUploads = this[UsersTable.storageMaxPendingUploads]
        val storageConfig = if (quotaBytes != null || maxUploadSizeBytes != null || maxPendingUploads != null) {
            UserStorageConfiguration(
                quotaBytes = quotaBytes,
                maxUploadSizeBytes = maxUploadSizeBytes,
                maxPendingUploads = maxPendingUploads
            )
        } else {
            null
        }

        return UserInternal(
            id = UserId(this[UsersTable.id].value),
            role = this[UsersTable.role],
            accountStatus = this[UsersTable.accountStatus],
            accountStatusOnRestore = this[UsersTable.accountStatusOnRestore],
            authorityLevel = this[UsersTable.authorityLevel],
            permissionCodes = this[UsersTable.permissionCodes].map { PermissionCode(it) }.toSet(),
            isTotpEnabled = this[UsersTable.isTotpEnabled],
            lastLoginAt = this[UsersTable.lastLoginAt]?.toKotlinInstant(),
            lastActiveAt = this[UsersTable.lastActiveAt]?.toKotlinInstant(),
            createdAt = this[UsersTable.createdAt].toKotlinInstant(),
            updatedAt = this[UsersTable.updatedAt]?.toKotlinInstant(),
            scheduledPermanentDeletionAt = this[UsersTable.scheduledPermanentDeletionAt]?.toKotlinInstant(),
            lockoutType = this[UsersTable.accountLockoutType],
            temporaryLockoutUntil = this[UsersTable.temporaryLockoutUntil]?.toKotlinInstant(),
            storageConfiguration = storageConfig,
            nickname = this[UsersTable.nickname],
            displayName = this[UsersTable.displayName],
            avatarFileId = this[UsersTable.avatarFileId]?.let { FileId(it) },
            avatarUrls = null,
            birthdate = this[UsersTable.birthdate]?.toKotlinInstant()
        )
    }
}
