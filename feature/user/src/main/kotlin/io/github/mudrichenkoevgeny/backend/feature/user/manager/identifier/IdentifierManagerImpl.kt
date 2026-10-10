package io.github.mudrichenkoevgeny.backend.feature.user.manager.identifier

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.mask.DataMasker
import io.github.mudrichenkoevgeny.backend.core.database.util.dbQuery
import io.github.mudrichenkoevgeny.backend.core.common.result.mapNotNullOrError
import io.github.mudrichenkoevgeny.backend.core.common.permission.PermissionRequirement
import io.github.mudrichenkoevgeny.backend.core.common.permission.PermissionSet
import io.github.mudrichenkoevgeny.backend.core.common.result.mapSuccess
import io.github.mudrichenkoevgeny.backend.feature.user.database.repository.useridentifier.UserIdentifierRepository
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.UserRoleAccessFilter
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.core.security.passwordhasher.PasswordHasher
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.SortOrder
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.permission.PermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierInternal
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierPrivate
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierSummary
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserSortValues
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.permission.IdentifierPermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.mapper.identifier.toUserIdentifierPrivate
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.mapper.identifier.toUserIdentifierSummary
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock

/**
 * Default [IdentifierManager] implementation.
 *
 * Wraps repository operations in [dbQuery] and uses [PasswordHasher] to hash passwords for password-based
 * providers before persisting identifier records via [UserIdentifierRepository].
 *
 * For management reads, applies role-scoped visibility and masked/unmasked output rules derived from
 * [IdentifierPermissionCode].
 */
@Singleton
class IdentifierManagerImpl @Inject constructor(
    private val passwordHasher: PasswordHasher,
    private val userManager: UserManager,
    private val userIdentifierRepository: UserIdentifierRepository
): IdentifierManager {

    override suspend fun getUserIdentifierInternalByProvider(
        userAuthProvider: UserAuthProvider,
        identifier: String
    ): AppResult<UserIdentifierInternal?> = dbQuery {
        userIdentifierRepository.getUserIdentifierInternalByProvider(
            userAuthProvider = userAuthProvider,
            identifier = identifier
        )
    }

    override suspend fun getUserIdentifierInternalById(
        userIdentifierId: UserIdentifierId
    ): AppResult<UserIdentifierInternal?> = dbQuery {
        userIdentifierRepository.getUserIdentifierInternalById(userIdentifierId)
    }

    override suspend fun getUserIdentifierPrivateById(
        userIdentifierId: UserIdentifierId
    ): AppResult<UserIdentifierPrivate?> = dbQuery {
        userIdentifierRepository.getUserIdentifierInternalById(userIdentifierId).mapSuccess { internal ->
            internal?.toUserIdentifierPrivate()
        }
    }

    override suspend fun getUserIdentifierPrivateById(
        userIdentifierId: UserIdentifierId,
        managementUserId: UserId,
        managementUserPermissionCodes: Set<PermissionCode>
    ): AppResult<UserIdentifierPrivate?> = dbQuery {
        val getIdentifierResult = userIdentifierRepository.getUserIdentifierInternalById(userIdentifierId)

        when (getIdentifierResult) {
            is AppResult.Error -> getIdentifierResult
            is AppResult.Success -> {
                val userIdentifierInternal = getIdentifierResult.data ?: return@dbQuery AppResult.Success(null)

                val getTargetUserResult = userManager.getUserInternalById(userIdentifierInternal.userId)
                    .mapNotNullOrError(UserError.UserNotFound(userIdentifierInternal.userId))

                when (getTargetUserResult) {
                    is AppResult.Error -> getTargetUserResult
                    is AppResult.Success -> {
                        val targetUser = getTargetUserResult.data
                        val accessFilter = buildAccessFilter(managementUserPermissionCodes)
                        if (targetUser.role !in accessFilter.allowedUserRoles) {
                            return@dbQuery AppResult.Error(UserError.UserMissingPermissions(managementUserId))
                        }

                        val permissionRequirement = determinePermissionRequirement(
                            userRole = targetUser.role,
                            userPermissionCodes = managementUserPermissionCodes
                        )

                        val userIdentifierPrivate = userIdentifierInternal.toUserIdentifierPrivate()

                        when (permissionRequirement) {
                            PermissionRequirement.UNMASKED -> AppResult.Success(userIdentifierPrivate)
                            PermissionRequirement.MASKED -> AppResult.Success(userIdentifierPrivate.maskSensitiveData())
                            PermissionRequirement.FORBIDDEN -> AppResult.Error(UserError.UserMissingPermissions(managementUserId))
                        }
                    }
                }
            }
        }
    }

    override suspend fun getUserIdentifiersInternalPageByUserId(
        userId: UserId
    ): AppResult<List<UserIdentifierInternal>> = dbQuery {
        userIdentifierRepository.getUserIdentifiersListByUserId(
            userId = userId
        )
    }

    override suspend fun createUserIdentifier(
        userId: UserId,
        userAuthProvider: UserAuthProvider,
        identifier: String,
        password: String?,
        externalProviderEmail: String?,
        externalProviderDisplayName: String?
    ): AppResult<UserIdentifierInternal> {
        val passwordHashResult = password?.let { passwordHasher.hash(it) }
        val passwordHash = when (passwordHashResult) {
            is AppResult.Success -> passwordHashResult.data
            is AppResult.Error -> return passwordHashResult
            null -> null
        }

        return dbQuery {
            val displayNameResult = createIdentifierDisplayName(
                userId = userId,
                userAuthProvider = userAuthProvider,
                identifier = identifier,
                externalProviderDisplayName = externalProviderDisplayName
            )
            
            val displayName = when (displayNameResult) {
                is AppResult.Success -> displayNameResult.data
                is AppResult.Error -> return@dbQuery displayNameResult
            }

            val userIdentifier = UserIdentifierInternal(
                userId = userId,
                userAuthProvider = userAuthProvider,
                identifier = identifier,
                displayName = displayName,
                passwordHash = passwordHash,
                externalProviderEmail = externalProviderEmail,
                createdAt = Clock.System.now(),
                updatedAt = null
            )

            userIdentifierRepository.createUserIdentifier(userIdentifier)
        }
    }

    override suspend fun deleteUserIdentifier(userIdentifierId: UserIdentifierId): AppResult<Unit> = dbQuery {
        userIdentifierRepository.deleteUserIdentifier(userIdentifierId)
    }

    override suspend fun updateUserIdentifierPassword(
        userIdentifier: UserIdentifierInternal,
        password: String
    ): AppResult<UserIdentifierPrivate> {
        val passwordHashResult = passwordHasher.hash(password)

        val passwordHash = when (passwordHashResult) {
            is AppResult.Success -> passwordHashResult.data
            is AppResult.Error -> return passwordHashResult
        }

        return dbQuery {
            userIdentifierRepository.updatePasswordHash(
                userIdentifier = userIdentifier,
                newPasswordHash = passwordHash
            ).mapSuccess { userIdentifierInternal -> userIdentifierInternal.toUserIdentifierPrivate() }
        }
    }

    override suspend fun clearUserIdentifierPassword(
        userIdentifier: UserIdentifierInternal
    ): AppResult<UserIdentifierPrivate> = dbQuery {
        userIdentifierRepository.updatePasswordHash(
            userIdentifier = userIdentifier,
            newPasswordHash = null
        ).mapSuccess { userIdentifierInternal -> userIdentifierInternal.toUserIdentifierPrivate() }
    }

    override suspend fun getUserIdentifiersSummaryPage(
        managementUserPermissionCodes: Set<PermissionCode>,
        pageParams: PageParams,
        sortBy: UserSortValues.UserIdentifierSortBy,
        sortOrder: SortOrder,
        userIds: List<UserId>,
        userAuthProviders: List<UserAuthProvider>,
        identifiers: List<String>
    ): AppResult<PagedResult<UserIdentifierSummary>> = dbQuery {
        val accessFilter = buildAccessFilter(managementUserPermissionCodes)

        val getIdentifiersResult = userIdentifierRepository.getUserIdentifiersPageWithAccessFilter(
            accessFilter = accessFilter,
            params = pageParams,
            sortBy = sortBy,
            sortOrder = sortOrder,
            userIds = userIds,
            userAuthProviders = userAuthProviders,
            identifiers = identifiers
        )

        when (getIdentifiersResult) {
            is AppResult.Error -> getIdentifiersResult
            is AppResult.Success -> {
                val paged = getIdentifiersResult.data
                val userRoleCache = mutableMapOf<UserId, UserRole>()

                val resultedItems = paged.items.mapNotNull { internal ->
                    val targetRole = userRoleCache[internal.userId] ?: run {
                        val targetUserResult = userManager.getUserInternalById(internal.userId)
                            .mapNotNullOrError(UserError.UserNotFound(internal.userId))

                        when (targetUserResult) {
                            is AppResult.Error -> return@dbQuery targetUserResult
                            is AppResult.Success -> targetUserResult.data.role.also {
                                userRoleCache[internal.userId] = it
                            }
                        }
                    }

                    val summary = internal.toUserIdentifierSummary()
                    when (determinePermissionRequirement(targetRole, managementUserPermissionCodes)) {
                        PermissionRequirement.UNMASKED -> summary
                        PermissionRequirement.MASKED -> summary.maskSensitiveData()
                        PermissionRequirement.FORBIDDEN -> null
                    }
                }

                AppResult.Success(
                    PagedResult(
                        items = resultedItems,
                        totalCount = paged.totalCount,
                        pageNumber = paged.pageNumber,
                        pageSize = paged.pageSize,
                        totalPages = paged.totalPages
                    )
                )
            }
        }
    }

    override suspend fun getUserIdentifiersSummaryPage(
        userId: UserId,
        pageParams: PageParams,
        sortBy: UserSortValues.UserIdentifierSortBy,
        sortOrder: SortOrder,
        userAuthProviders: List<UserAuthProvider>,
        identifiers: List<String>
    ): AppResult<PagedResult<UserIdentifierSummary>> = dbQuery {
        userIdentifierRepository.getUserIdentifiersPageByUserId(
            userId = userId,
            params = pageParams,
            sortBy = sortBy,
            sortOrder = sortOrder,
            userAuthProviders = userAuthProviders,
            identifiers = identifiers
        ).mapSuccess { paged ->
            PagedResult(
                items = paged.items.map { it.toUserIdentifierSummary() },
                totalCount = paged.totalCount,
                pageNumber = paged.pageNumber,
                pageSize = paged.pageSize,
                totalPages = paged.totalPages
            )
        }
    }

    private fun determinePermissionRequirement(
        userRole: UserRole,
        userPermissionCodes: Set<PermissionCode>
    ): PermissionRequirement {
        val permissionSet = when (userRole) {
            UserRole.USER -> PermissionSet(
                masked = IdentifierPermissionCode.IDENTIFIER_GET_OF_USER_MASKED,
                unmasked = IdentifierPermissionCode.IDENTIFIER_GET_OF_USER_UNMASKED
            )
            UserRole.STAFF -> PermissionSet(
                masked = IdentifierPermissionCode.IDENTIFIER_GET_OF_STAFF_MASKED,
                unmasked = IdentifierPermissionCode.IDENTIFIER_GET_OF_STAFF_UNMASKED
            )
            UserRole.ADMIN -> null
        } ?: return PermissionRequirement.FORBIDDEN

        return when {
            permissionSet.unmasked in userPermissionCodes -> PermissionRequirement.UNMASKED
            permissionSet.masked in userPermissionCodes -> PermissionRequirement.MASKED
            else -> PermissionRequirement.FORBIDDEN
        }
    }

    private fun buildAccessFilter(userPermissionCodes: Set<PermissionCode>): UserRoleAccessFilter {
        val allowedUserRoles = mutableSetOf<UserRole>()

        if (IdentifierPermissionCode.IDENTIFIER_GET_OF_USER_MASKED in userPermissionCodes ||
            IdentifierPermissionCode.IDENTIFIER_GET_OF_USER_UNMASKED in userPermissionCodes
        ) {
            allowedUserRoles.add(UserRole.USER)
        }
        if (IdentifierPermissionCode.IDENTIFIER_GET_OF_STAFF_MASKED in userPermissionCodes ||
            IdentifierPermissionCode.IDENTIFIER_GET_OF_STAFF_UNMASKED in userPermissionCodes
        ) {
            allowedUserRoles.add(UserRole.STAFF)
        }

        return UserRoleAccessFilter(allowedUserRoles = allowedUserRoles)
    }

    private fun UserIdentifierPrivate.maskSensitiveData(): UserIdentifierPrivate = copy(
        identifier = when (userAuthProvider) {
            UserAuthProvider.EMAIL -> DataMasker.maskEmail(identifier)
            UserAuthProvider.PHONE -> DataMasker.maskPhone(identifier)
            else -> DataMasker.maskId(identifier)
        },
        isSensitiveValuesMasked = true
    )

    private fun UserIdentifierSummary.maskSensitiveData(): UserIdentifierSummary = copy(
        identifier = when (userAuthProvider) {
            UserAuthProvider.EMAIL -> DataMasker.maskEmail(identifier)
            UserAuthProvider.PHONE -> DataMasker.maskPhone(identifier)
            else -> DataMasker.maskId(identifier)
        }
    )

    private suspend fun createIdentifierDisplayName(
        userId: UserId,
        userAuthProvider: UserAuthProvider,
        identifier: String,
        externalProviderDisplayName: String?
    ): AppResult<String> {
        return when (userAuthProvider) {
            UserAuthProvider.EMAIL, UserAuthProvider.PHONE -> AppResult.Success(identifier)
            else -> {
                if (!externalProviderDisplayName.isNullOrBlank()) {
                    AppResult.Success(externalProviderDisplayName)
                } else {
                    generateUniqueFallbackDisplayName(
                        userId = userId,
                        userAuthProvider = userAuthProvider
                    )
                }
            }
        }
    }

    private suspend fun generateUniqueFallbackDisplayName(
        userId: UserId,
        userAuthProvider: UserAuthProvider
    ): AppResult<String> {
        val baseName = userAuthProvider.serialName.replaceFirstChar { it.uppercaseChar() }

        val existingIdentifiersResult = userIdentifierRepository.getUserIdentifiersListByUserId(userId)
        val existingIdentifiers = when (existingIdentifiersResult) {
            is AppResult.Success -> existingIdentifiersResult.data
            is AppResult.Error -> return existingIdentifiersResult
        }

        val existingDisplayNames = existingIdentifiers.mapTo(HashSet()) { it.displayName }

        if (!existingDisplayNames.contains(baseName)) {
            return AppResult.Success(baseName)
        }

        var counter = 2
        while (existingDisplayNames.contains("$baseName $counter")) {
            counter++
        }

        return AppResult.Success("$baseName $counter")
    }
}
