package io.github.mudrichenkoevgeny.backend.feature.user.manager.identifier

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.mask.DataMasker
import io.github.mudrichenkoevgeny.backend.core.database.util.dbQuery
import io.github.mudrichenkoevgeny.backend.core.common.result.mapNotNullOrError
import io.github.mudrichenkoevgeny.backend.core.common.permission.PermissionRequirement
import io.github.mudrichenkoevgeny.backend.core.common.permission.PermissionSet
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
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserSortValues
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.permission.IdentifierPermissionCode
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

    override suspend fun getUserIdentifier(
        userAuthProvider: UserAuthProvider,
        identifier: String
    ): AppResult<UserIdentifierInternal?> = dbQuery {
        userIdentifierRepository.getUserIdentifier(
            userAuthProvider = userAuthProvider,
            identifier = identifier
        )
    }

    override suspend fun getUserIdentifierById(
        userIdentifierId: UserIdentifierId,
        userId: UserId,
        userPermissionCodes: Set<PermissionCode>
    ): AppResult<UserIdentifierInternal?> = dbQuery {
        val getIdentifierResult = userIdentifierRepository.getUserIdentifierById(userIdentifierId)

        when (getIdentifierResult) {
            is AppResult.Error -> getIdentifierResult
            is AppResult.Success -> {
                val userIdentifier = getIdentifierResult.data ?: return@dbQuery AppResult.Success(null)

                val getTargetUserResult = userManager.getUserById(userIdentifier.userId)
                    .mapNotNullOrError(UserError.UserNotFound(userIdentifier.userId))

                when (getTargetUserResult) {
                    is AppResult.Error -> getTargetUserResult
                    is AppResult.Success -> {
                        val targetUser = getTargetUserResult.data
                        val accessFilter = buildAccessFilter(userPermissionCodes)
                        if (targetUser.role !in accessFilter.allowedUserRoles) {
                            return@dbQuery AppResult.Error(UserError.UserMissingPermissions(userId))
                        }

                        val permissionRequirement = determinePermissionRequirement(
                            userRole = targetUser.role,
                            userPermissionCodes = userPermissionCodes
                        )

                        when (permissionRequirement) {
                            PermissionRequirement.UNMASKED -> AppResult.Success(userIdentifier)
                            PermissionRequirement.MASKED -> AppResult.Success(userIdentifier.maskSensitiveData())
                            PermissionRequirement.FORBIDDEN -> AppResult.Error(UserError.UserMissingPermissions(userId))
                        }
                    }
                }
            }
        }
    }

    override suspend fun getUserIdentifiersByUserId(
        userId: UserId
    ): AppResult<List<UserIdentifierInternal>> = dbQuery {
        userIdentifierRepository.getUserIdentifiersListByUserId(
            userId = userId
        )
    }

    override suspend fun getUserIdentifiersList(
        userPermissionCodes: Set<PermissionCode>,
        pageParams: PageParams,
        sortBy: UserSortValues.UserIdentifierSortBy,
        sortOrder: SortOrder,
        userIds: List<UserId>,
        userAuthProviders: List<UserAuthProvider>,
        identifiers: List<String>
    ): AppResult<PagedResult<UserIdentifierInternal>> = dbQuery {
        val accessFilter = buildAccessFilter(userPermissionCodes)

        val getIdentifiersResult = userIdentifierRepository.getUserIdentifiersList(
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

                val finalItems = paged.items.mapNotNull { identifier ->
                    val targetRole = userRoleCache[identifier.userId] ?: run {
                        val targetUserResult = userManager.getUserById(identifier.userId)
                            .mapNotNullOrError(UserError.UserNotFound(identifier.userId))

                        when (targetUserResult) {
                            is AppResult.Error -> return@dbQuery targetUserResult
                            is AppResult.Success -> targetUserResult.data.role.also {
                                userRoleCache[identifier.userId] = it
                            }
                        }
                    }

                    when (determinePermissionRequirement(targetRole, userPermissionCodes)) {
                        PermissionRequirement.UNMASKED -> identifier
                        PermissionRequirement.MASKED -> identifier.maskSensitiveData()
                        PermissionRequirement.FORBIDDEN -> null
                    }
                }

                AppResult.Success(paged.copy(items = finalItems))
            }
        }
    }

    override suspend fun createUserIdentifier(
        userId: UserId,
        userAuthProvider: UserAuthProvider,
        identifier: String,
        password: String?
    ): AppResult<UserIdentifierInternal> = dbQuery {
        val passwordHash = password?.let { password ->
            val passwordHashResult = passwordHasher.hash(password)

            when (passwordHashResult) {
                is AppResult.Success -> passwordHashResult.data
                is AppResult.Error -> return@dbQuery passwordHashResult
            }
        }

        val userIdentifier = UserIdentifierInternal(
            userId = userId,
            userAuthProvider = userAuthProvider,
            identifier = identifier,
            passwordHash = passwordHash,
            isSensitiveValuesMasked = false,
            createdAt = Clock.System.now(),
            updatedAt = null
        )

        userIdentifierRepository.createUserIdentifier(userIdentifier)
    }

    override suspend fun deleteUserIdentifier(userIdentifierId: UserIdentifierId): AppResult<Unit> = dbQuery {
        userIdentifierRepository.deleteUserIdentifier(userIdentifierId)
    }

    override suspend fun updateUserIdentifierPassword(
        userIdentifier: UserIdentifierInternal,
        identifier: String,
        password: String
    ): AppResult<UserIdentifierInternal> = dbQuery {
        val passwordHashResult = passwordHasher.hash(password)

        val passwordHash = when (passwordHashResult) {
            is AppResult.Success -> passwordHashResult.data
            is AppResult.Error -> return@dbQuery passwordHashResult
        }

        userIdentifierRepository.updateUserIdentifier(
            userIdentifier = userIdentifier,
            identifier = identifier,
            passwordHash = passwordHash
        )
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

    private fun UserIdentifierInternal.maskSensitiveData(): UserIdentifierInternal = copy(
        identifier = when (userAuthProvider) {
            UserAuthProvider.EMAIL -> DataMasker.maskEmail(identifier)
            UserAuthProvider.PHONE -> DataMasker.maskPhone(identifier)
            else -> DataMasker.maskId(identifier)
        },
        passwordHash = passwordHash?.let(DataMasker::maskFullValue),
        isSensitiveValuesMasked = true
    )
}