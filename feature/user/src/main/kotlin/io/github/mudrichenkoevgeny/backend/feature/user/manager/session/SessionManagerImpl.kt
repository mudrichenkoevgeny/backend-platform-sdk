package io.github.mudrichenkoevgeny.backend.feature.user.manager.session

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.mask.DataMasker
import io.github.mudrichenkoevgeny.backend.core.common.permission.PermissionRequirement
import io.github.mudrichenkoevgeny.backend.core.common.permission.PermissionSet
import io.github.mudrichenkoevgeny.backend.core.common.result.mapNotNullOrError
import io.github.mudrichenkoevgeny.backend.core.database.util.dbQuery
import io.github.mudrichenkoevgeny.backend.feature.user.config.model.UserConfig
import io.github.mudrichenkoevgeny.backend.feature.user.database.repository.usersession.UserSessionRepository
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.UserRoleAccessFilter
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.provider.authsettings.AuthSettingsProvider
import io.github.mudrichenkoevgeny.backend.feature.user.security.refreshtokenprovider.RefreshTokenProvider
import io.github.mudrichenkoevgeny.backend.feature.user.security.tokenprovider.TokenProvider
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientType
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientInfo
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.SortOrder
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.permission.PermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserSortValues
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.DeletedSessions
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSession
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionInternal
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.token.RefreshToken
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.token.SessionToken
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.permission.SessionPermissionCode
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Default [SessionManager] implementation.
 *
 * Generates access tokens via [TokenProvider] and refresh tokens via [RefreshTokenProvider], persists session
 * records through [UserSessionRepository], and performs refresh/revocation operations inside [dbQuery].
 *
 * Uses [UserConfig] token validity settings to compute token expiration timestamps.
 */
@Singleton
class SessionManagerImpl @Inject constructor(
    private val authSettingsProvider: AuthSettingsProvider,
    private val jwtTokenProvider: TokenProvider,
    private val refreshTokenProvider: RefreshTokenProvider,
    private val userManager: UserManager,
    private val userSessionRepository: UserSessionRepository
) : SessionManager {

    override suspend fun createSession(
        userId: UserId,
        userRole: UserRole,
        identifier: String,
        identifierId: UserIdentifierId,
        identifierAuthProvider: UserAuthProvider,
        clientInfo: ClientInfo,
        lastReauthenticatedAt: Instant
    ): AppResult<SessionToken> = dbQuery {
        val userSessionId = UserSessionId.generate()

        val now = Clock.System.now()
        val accessExpiry = now + authSettingsProvider.getAccessTokenExpirationSeconds().seconds
        val refreshExpiry = now + authSettingsProvider.getRefreshTokenExpirationSeconds().seconds

        val accessTokenResult = jwtTokenProvider.generateAccessToken(
            userId = userId,
            userRole = userRole,
            sessionId = userSessionId,
            issuedAt = now,
            expiration = accessExpiry
        )
        val accessToken = when (accessTokenResult) {
            is AppResult.Success -> accessTokenResult.data
            is AppResult.Error -> return@dbQuery accessTokenResult
        }

        val refreshTokenResult = refreshTokenProvider.getRefreshToken()

        val refreshToken = when (refreshTokenResult) {
            is AppResult.Success -> refreshTokenResult.data
            is AppResult.Error -> return@dbQuery refreshTokenResult
        }

        val refreshTokenHashResult = refreshTokenProvider.getRefreshTokenHash(refreshToken)

        val refreshTokenHash = when (refreshTokenHashResult) {
            is AppResult.Success -> refreshTokenHashResult.data
            is AppResult.Error -> return@dbQuery refreshTokenHashResult
        }

        val userSession = UserSessionInternal(
            id = userSessionId,
            userId = userId,
            userRole = userRole,
            identifier = identifier,
            identifierId = identifierId,
            identifierAuthProvider = identifierAuthProvider,
            refreshTokenHash = refreshTokenHash,
            deviceInfo = clientInfo.deviceInfo,
            userAgent = clientInfo.userAgent,
            ipAddress = clientInfo.ipAddress,
            expiresAt = refreshExpiry,
            lastAccessedAt = now,
            lastReauthenticatedAt = lastReauthenticatedAt,
            createdAt = now,
            updatedAt = null
        )

        val createUserSessionResult = userSessionRepository.createUserSession(userSession)
        when (createUserSessionResult) {
            is AppResult.Success -> AppResult.Success(
                SessionToken(
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    expiresAt = createUserSessionResult.data.expiresAt
                )
            )
            is AppResult.Error -> createUserSessionResult
        }
    }

    override suspend fun refreshSession(
        refreshToken: RefreshToken,
        clientInfo: ClientInfo
    ): AppResult<SessionToken> = dbQuery {
        val refreshTokenHashResult = refreshTokenProvider.getRefreshTokenHash(refreshToken)

        val refreshTokenHash = when (refreshTokenHashResult) {
            is AppResult.Success -> refreshTokenHashResult.data
            is AppResult.Error -> return@dbQuery refreshTokenHashResult
        }

        val currentUserSessionResult = userSessionRepository.getUserSessionByHash(
            refreshTokenHash = refreshTokenHash
        )
        val currentUserSession = when (currentUserSessionResult) {
            is AppResult.Success -> currentUserSessionResult.data
            is AppResult.Error -> return@dbQuery currentUserSessionResult
        }

        val isSessionValid = currentUserSession?.isValid(
            clientDeviceId = clientInfo.deviceInfo.deviceId,
            now = Clock.System.now()
        ) ?: false

        if (currentUserSession == null || !isSessionValid) {
            return@dbQuery AppResult.Error(UserError.InvalidRefreshToken())
        }

        userSessionRepository.deleteUserSessionById(currentUserSession.id)

        createSession(
            userId = currentUserSession.userId,
            userRole = currentUserSession.userRole,
            identifier = currentUserSession.identifier,
            identifierId = currentUserSession.identifierId,
            identifierAuthProvider = currentUserSession.identifierAuthProvider,
            clientInfo = clientInfo,
            lastReauthenticatedAt = currentUserSession.lastReauthenticatedAt
        )
    }

    override suspend fun updateLastAccessed(userSessionId: UserSessionId): AppResult<Unit> = dbQuery {
        userSessionRepository.updateLastAccessed(userSessionId)
    }

    override suspend fun updateLastReauthenticated(userSessionId: UserSessionId): AppResult<Unit> = dbQuery {
        userSessionRepository.updateLastReauthenticated(userSessionId)
    }

    override suspend fun deleteSessionById(userSessionId: UserSessionId): AppResult<Unit> = dbQuery {
        userSessionRepository.deleteUserSessionById(userSessionId)
    }

    override suspend fun deleteAllUserSessions(userId: UserId): AppResult<Unit> = dbQuery {
        userSessionRepository.deleteAllUserSessions(userId)
    }

    override suspend fun getUserSessionForSystem(userSessionId: UserSessionId): AppResult<UserSessionInternal?> = dbQuery {
        userSessionRepository.getUserSessionInternalById(userSessionId)
    }

    override suspend fun getUserSessionForSelf(userSessionId: UserSessionId): AppResult<UserSession?> = dbQuery {
        userSessionRepository.getUserSessionById(userSessionId)
    }

    override suspend fun getAllUserSessions(userId: UserId): AppResult<List<UserSessionInternal>> = dbQuery {
        userSessionRepository.getAllUserSessions(userId)
    }

    override suspend fun getUserSessionsByIdentifierId(
        userIdentifierId: UserIdentifierId,
        userId: UserId?
    ): AppResult<List<UserSessionInternal>> = dbQuery {
        userSessionRepository.getUserSessionsByIdentifierId(
            userIdentifierId = userIdentifierId,
            userId = userId
        )
    }

    override suspend fun deleteAllSessionsExceptOneForSelf(
        userId: UserId,
        userSessionId: UserSessionId
    ): AppResult<DeletedSessions> = dbQuery {
        val deleteSessionsResult = userSessionRepository.deleteAllUserSessionsExceptOne(userId, userSessionId)

        when (deleteSessionsResult) {
            is AppResult.Error -> deleteSessionsResult
            is AppResult.Success -> AppResult.Success(
                DeletedSessions(
                    deletedSessionIds = deleteSessionsResult.data
                )
            )
        }
    }

    override suspend fun deleteLeastRecentlyUsedUserSession(userId: UserId): AppResult<UserSessionId> = dbQuery {
        userSessionRepository.deleteLeastRecentlyUsedUserSession(userId)
    }

    override suspend fun getUserSessionForManagement(
        userSessionId: UserSessionId,
        managementUserId: UserId,
        managementUserPermissionCodes: Set<PermissionCode>
    ): AppResult<UserSession?> = dbQuery {
        val getSessionResult = userSessionRepository.getUserSessionById(userSessionId)

        when (getSessionResult) {
            is AppResult.Error -> getSessionResult
            is AppResult.Success -> {
                val userSession = getSessionResult.data ?: return@dbQuery AppResult.Success(null)
                val getTargetUserResult = userManager.getUserByIdForSelf(userSession.userId)
                    .mapNotNullOrError(UserError.UserNotFound(userSession.userId))

                when (getTargetUserResult) {
                    is AppResult.Error -> getTargetUserResult
                    is AppResult.Success -> {
                        val targetUser = getTargetUserResult.data
                        val accessFilter = buildAccessFilter(managementUserPermissionCodes)
                        if (targetUser.role !in accessFilter.allowedUserRoles) {
                            return@dbQuery AppResult.Error(UserError.UserMissingPermissions(managementUserId))
                        }

                        when (determinePermissionRequirement(targetUser.role, managementUserPermissionCodes)) {
                            PermissionRequirement.UNMASKED -> AppResult.Success(userSession)
                            PermissionRequirement.MASKED -> AppResult.Success(userSession.maskSensitiveData())
                            PermissionRequirement.FORBIDDEN -> AppResult.Error(UserError.UserMissingPermissions(managementUserId))
                        }
                    }
                }
            }
        }
    }

    override suspend fun getSessionsPageForManagement(
        managementUserPermissionCodes: Set<PermissionCode>,
        pageParams: PageParams,
        sortBy: UserSortValues.UserSessionSortBy,
        sortOrder: SortOrder,
        userIds: List<UserId>,
        userRoles: List<UserRole>,
        identifiers: List<String>,
        identifierIds: List<UserIdentifierId>,
        identifierAuthProviders: List<UserAuthProvider>,
        clientTypes: List<ClientType>,
        userAgents: List<String>,
        ipAddresses: List<String>,
        languages: List<String>,
        deviceIds: List<String>,
        deviceNames: List<String>,
        appVersions: List<String>,
        operationSystemVersions: List<String>
    ): AppResult<PagedResult<UserSession>> = dbQuery {
        val accessFilter = buildAccessFilter(managementUserPermissionCodes)

        val getSessionsResult = userSessionRepository.getUserSessionsPageWithAccessFilter(
            accessFilter = accessFilter,
            pageParams = pageParams,
            sortBy = sortBy,
            sortOrder = sortOrder,
            userIds = userIds,
            userRoles = userRoles,
            identifiers = identifiers,
            identifierIds = identifierIds,
            identifierAuthProviders = identifierAuthProviders,
            clientTypes = clientTypes,
            userAgents = userAgents,
            ipAddresses = ipAddresses,
            languages = languages,
            deviceIds = deviceIds,
            deviceNames = deviceNames,
            appVersions = appVersions,
            operationSystemVersions = operationSystemVersions
        )

        when (getSessionsResult) {
            is AppResult.Error -> getSessionsResult
            is AppResult.Success -> {
                val paged = getSessionsResult.data
                val userRoleCache = mutableMapOf<UserId, UserRole>()

                val resultedItems = paged.items.mapNotNull { session ->
                    val targetRole = userRoleCache[session.userId] ?: run {
                        val targetUserResult = userManager.getUserByIdForSelf(session.userId)
                            .mapNotNullOrError(UserError.UserNotFound(session.userId))

                        when (targetUserResult) {
                            is AppResult.Error -> return@dbQuery targetUserResult
                            is AppResult.Success -> targetUserResult.data.role.also {
                                userRoleCache[session.userId] = it
                            }
                        }
                    }

                    when (determinePermissionRequirement(targetRole, managementUserPermissionCodes)) {
                        PermissionRequirement.UNMASKED -> session
                        PermissionRequirement.MASKED -> session.maskSensitiveData()
                        PermissionRequirement.FORBIDDEN -> null
                    }
                }

                AppResult.Success(paged.copy(items = resultedItems))
            }
        }
    }

    override suspend fun getSessionsPageForSelf(
        userId: UserId,
        pageParams: PageParams,
        sortBy: UserSortValues.UserSessionSortBy,
        sortOrder: SortOrder,
        identifiers: List<String>,
        identifierIds: List<UserIdentifierId>,
        identifierAuthProviders: List<UserAuthProvider>,
        clientTypes: List<ClientType>,
        userAgents: List<String>,
        ipAddresses: List<String>,
        languages: List<String>,
        deviceIds: List<String>,
        deviceNames: List<String>,
        appVersions: List<String>,
        operationSystemVersions: List<String>
    ): AppResult<PagedResult<UserSession>> = dbQuery {
        userSessionRepository.getUserSessionsPageByUserId(
            userId = userId,
            pageParams = pageParams,
            sortBy = sortBy,
            sortOrder = sortOrder,
            identifiers = identifiers,
            identifierIds = identifierIds,
            identifierAuthProviders = identifierAuthProviders,
            clientTypes = clientTypes,
            userAgents = userAgents,
            ipAddresses = ipAddresses,
            languages = languages,
            deviceIds = deviceIds,
            deviceNames = deviceNames,
            appVersions = appVersions,
            operationSystemVersions = operationSystemVersions
        )
    }

    private fun determinePermissionRequirement(
        userRole: UserRole,
        userPermissionCodes: Set<PermissionCode>
    ): PermissionRequirement {
        val permissionSet = when (userRole) {
            UserRole.USER -> PermissionSet(
                masked = SessionPermissionCode.SESSION_GET_OF_USER_MASKED,
                unmasked = SessionPermissionCode.SESSION_GET_OF_USER_UNMASKED
            )
            UserRole.STAFF -> PermissionSet(
                masked = SessionPermissionCode.SESSION_GET_OF_STAFF_MASKED,
                unmasked = SessionPermissionCode.SESSION_GET_OF_STAFF_UNMASKED
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

        if (SessionPermissionCode.SESSION_GET_OF_USER_MASKED in userPermissionCodes ||
            SessionPermissionCode.SESSION_GET_OF_USER_UNMASKED in userPermissionCodes
        ) {
            allowedUserRoles.add(UserRole.USER)
        }
        if (SessionPermissionCode.SESSION_GET_OF_STAFF_MASKED in userPermissionCodes ||
            SessionPermissionCode.SESSION_GET_OF_STAFF_UNMASKED in userPermissionCodes
        ) {
            allowedUserRoles.add(UserRole.STAFF)
        }

        return UserRoleAccessFilter(allowedUserRoles = allowedUserRoles)
    }

    private fun UserSession.maskSensitiveData(): UserSession = copy(
        identifier = when (identifierAuthProvider) {
            UserAuthProvider.EMAIL -> DataMasker.maskEmail(identifier)
            UserAuthProvider.PHONE -> DataMasker.maskPhone(identifier)
            else -> DataMasker.maskId(identifier)
        },
        userAgent = userAgent?.let { userAgent -> DataMasker.maskPartialValue(userAgent) },
        ipAddress = ipAddress?.let { ipAddress -> DataMasker.maskIpAddress(ipAddress) },
        deviceInfo = deviceInfo.copy(
            deviceName = deviceInfo.deviceName?.let { deviceName ->
                DataMasker.maskPartialValue(deviceName)
            },
            language = deviceInfo.language?.let { language ->
                DataMasker.maskPartialValue(language)
            },
            appVersion = deviceInfo.appVersion?.let { appVersion ->
                DataMasker.maskPartialValue(appVersion)
            },
            operationSystemVersion = deviceInfo.operationSystemVersion?.let { osVersion ->
                DataMasker.maskPartialValue(osVersion)
            }
        ),
        isSensitiveValuesMasked = true
    )
}