package io.github.mudrichenkoevgeny.backend.feature.user.manager.session

import io.github.mudrichenkoevgeny.backend.core.common.di.qualifiers.BackgroundScope
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.mask.DataMasker
import io.github.mudrichenkoevgeny.backend.core.common.model.UpdateField
import io.github.mudrichenkoevgeny.backend.core.common.permission.PermissionRequirement
import io.github.mudrichenkoevgeny.backend.core.common.permission.PermissionSet
import io.github.mudrichenkoevgeny.backend.core.common.result.mapNotNullOrError
import io.github.mudrichenkoevgeny.backend.feature.user.error.validation.validateRoleAndStatus
import io.github.mudrichenkoevgeny.backend.core.database.manager.redis.RedisManager
import io.github.mudrichenkoevgeny.backend.core.database.util.dbQuery
import io.github.mudrichenkoevgeny.backend.core.security.settings.provider.SecuritySettingsProvider
import io.github.mudrichenkoevgeny.backend.feature.user.config.model.UserConfig
import io.github.mudrichenkoevgeny.backend.feature.user.database.repository.user.UserRepository
import io.github.mudrichenkoevgeny.backend.feature.user.database.repository.usersession.UserSessionRepository
import io.github.mudrichenkoevgeny.backend.feature.user.database.repository.userknowndevices.UserKnownDevicesRepository
import io.github.mudrichenkoevgeny.backend.feature.user.database.repository.useridentifier.UserIdentifierRepository
import io.github.mudrichenkoevgeny.backend.feature.user.model.device.UserKnownDevice
import io.github.mudrichenkoevgeny.backend.feature.user.service.email.EmailService
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.UserRoleAccessFilter
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.token.RotatedRefreshTokenData
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.provider.authsettings.AuthSettingsProvider
import io.github.mudrichenkoevgeny.backend.feature.user.security.refreshtokenprovider.RefreshTokenProvider
import io.github.mudrichenkoevgeny.backend.feature.user.security.tokenprovider.TokenProvider
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.AuditEventMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientType
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientInfo
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.SortOrder
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.permission.PermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.core.common.serialization.FoundationJson
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.metadata.UserAuditMetadataKey
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
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
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
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
    private val appLogger: AppLogger,
    private val authSettingsProvider: AuthSettingsProvider,
    private val securitySettingsProvider: SecuritySettingsProvider,
    private val jwtTokenProvider: TokenProvider,
    private val refreshTokenProvider: RefreshTokenProvider,
    private val userManager: UserManager,
    private val userSessionRepository: UserSessionRepository,
    private val userKnownDevicesRepository: UserKnownDevicesRepository,
    private val userIdentifierRepository: UserIdentifierRepository,
    private val emailService: EmailService,
    private val redisManager: RedisManager,
    private val auditLogger: AuditLogger,
    private val userRepository: UserRepository,
    @param:BackgroundScope private val scope: CoroutineScope
) : SessionManager {

    override suspend fun createSession(
        userId: UserId,
        userRole: UserRole,
        identifier: String,
        identifierId: UserIdentifierId,
        identifierAuthProvider: UserAuthProvider,
        clientInfo: ClientInfo,
        lastReauthenticatedAt: Instant,
        checkNewDevice: Boolean
    ): AppResult<SessionToken> {
        var isNewDeviceDetected = false

        val sessionResult = createSessionInDatabase(
            userId = userId,
            userRole = userRole,
            identifier = identifier,
            identifierId = identifierId,
            identifierAuthProvider = identifierAuthProvider,
            clientInfo = clientInfo,
            lastReauthenticatedAt = lastReauthenticatedAt,
            checkNewDevice = checkNewDevice,
            onNewDeviceDetected = { isNewDeviceDetected = true }
        )

        if (sessionResult is AppResult.Success && isNewDeviceDetected) {
            scope.launch {
                notifyNewDeviceDetected(userId, userRole, clientInfo)
            }
        }

        return sessionResult
    }

    private suspend fun createSessionInDatabase(
        userId: UserId,
        userRole: UserRole,
        identifier: String,
        identifierId: UserIdentifierId,
        identifierAuthProvider: UserAuthProvider,
        clientInfo: ClientInfo,
        lastReauthenticatedAt: Instant,
        checkNewDevice: Boolean,
        onNewDeviceDetected: () -> Unit
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

        if (checkNewDevice) {
            val deviceId = resolveDeviceId(userId, clientInfo)
            val knownDeviceResult = userKnownDevicesRepository.getKnownDevice(userId, deviceId)
            when (knownDeviceResult) {
                is AppResult.Success -> {
                    val knownDevice = knownDeviceResult.data
                    if (knownDevice != null) {
                        userKnownDevicesRepository.updateKnownDevice(
                            userId = userId,
                            deviceId = deviceId,
                            lastIpAddress = UpdateField.Set(clientInfo.ipAddress),
                            lastSeenAt = UpdateField.Set(now)
                        )
                    } else {
                        val newDevice = UserKnownDevice(
                            userId = userId,
                            deviceId = deviceId,
                            firstIpAddress = clientInfo.ipAddress,
                            lastIpAddress = clientInfo.ipAddress,
                            userAgent = clientInfo.userAgent,
                            firstSeenAt = now,
                            lastSeenAt = now
                        )
                        userKnownDevicesRepository.createKnownDevice(newDevice)
                        onNewDeviceDetected()
                    }
                }
                is AppResult.Error -> {
                    appLogger.logError(knownDeviceResult.error)
                }
            }
        }

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

    private fun resolveDeviceId(userId: UserId, clientInfo: ClientInfo): String {
        val rawDeviceId = clientInfo.deviceInfo.deviceId?.asHexDashString()
        if (!rawDeviceId.isNullOrBlank()) {
            return rawDeviceId
        }

        val ip = clientInfo.ipAddress ?: "unknown_ip"
        val userAgent = clientInfo.userAgent ?: "unknown_ua"
        val rawString = "${userId.value}:$ip:$userAgent"
        val hashBytes = MessageDigest.getInstance("SHA-256").digest(rawString.toByteArray())
        val hexString = hashBytes.joinToString("") { "%02x".format(it) }
        return "synthetic:$hexString"
    }

    private suspend fun notifyNewDeviceDetected(
        userId: UserId,
        userRole: UserRole,
        clientInfo: ClientInfo
    ) {
        val userIdentifiersResult = userIdentifierRepository.getUserIdentifiersListByUserId(userId)
        if (userIdentifiersResult is AppResult.Success) {
            val emailIdentifier = userIdentifiersResult.data.find { it.userAuthProvider == UserAuthProvider.EMAIL }
            if (emailIdentifier != null) {
                emailService.sendNewDeviceLoginEmail(
                    email = emailIdentifier.identifier,
                    ipAddress = clientInfo.ipAddress,
                    deviceName = clientInfo.deviceInfo.deviceName,
                    userAgent = clientInfo.userAgent
                )
            }
        }

        auditLogger.log(
            actorId = userId.asHexDashString(),
            actorType = AuditActorType.USER,
            actorUserRole = userRole.serialName,
            action = UserAuditActionType.NEW_DEVICE_DETECTED,
            resource = UserAuditResourceType.USER,
            resourceId = userId.asHexDashString(),
            status = AuditStatus.SUCCESS,
            metadata = setOf()
        )
    }

    override suspend fun refreshSession(
        refreshToken: RefreshToken,
        clientInfo: ClientInfo,
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
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

        val now = Clock.System.now()

        val isSessionValid = currentUserSession?.isValid(
            clientDeviceId = clientInfo.deviceInfo.deviceId,
            now = now
        ) ?: false

        if (currentUserSession != null && isSessionValid) {
            val userResult = userRepository.getUserDetailsById(currentUserSession.userId)
                .mapNotNullOrError(UserError.UserNotFound())

            val user = when (userResult) {
                is AppResult.Success -> userResult.data
                is AppResult.Error -> return@dbQuery userResult
            }

            user.validateRoleAndStatus(allowedRoles, allowedAccountStatuses)?.let { error ->
                return@dbQuery AppResult.Error(error)
            }

            userSessionRepository.deleteUserSessionById(currentUserSession.id)

            val newSessionResult = createSession(
                userId = currentUserSession.userId,
                userRole = currentUserSession.userRole,
                identifier = currentUserSession.identifier,
                identifierId = currentUserSession.identifierId,
                identifierAuthProvider = currentUserSession.identifierAuthProvider,
                clientInfo = clientInfo,
                lastReauthenticatedAt = currentUserSession.lastReauthenticatedAt,
                checkNewDevice = false
            )

            if (newSessionResult is AppResult.Success) {
                val rotatedData = RotatedRefreshTokenData.from(
                    sessionToken = newSessionResult.data,
                    userId = currentUserSession.userId,
                    rotatedAt = now
                )
                val serializedData = FoundationJson.encodeToString(rotatedData)
                val refreshTtl = authSettingsProvider.getRefreshTokenExpirationSeconds().toLong()
                redisManager.setWithExpiration(
                    key = buildRotatedRefreshKey(refreshTokenHash.value),
                    value = serializedData,
                    expirationSeconds = refreshTtl
                )
            }

            return@dbQuery newSessionResult
        }

        val rotatedRedisResult = redisManager.get(buildRotatedRefreshKey(refreshTokenHash.value))
        val rotatedJson = when (rotatedRedisResult) {
            is AppResult.Success -> rotatedRedisResult.data
            is AppResult.Error -> null
        }

        if (rotatedJson != null) {
            val rotatedData = runCatching {
                FoundationJson.decodeFromString<RotatedRefreshTokenData>(rotatedJson)
            }.getOrNull()

            if (rotatedData != null) {
                val elapsed = now - rotatedData.rotatedAt
                val gracePeriod = securitySettingsProvider.getRefreshTokenRotationGracePeriodSeconds().seconds
                if (elapsed <= gracePeriod) {
                    return@dbQuery AppResult.Success(rotatedData.toSessionToken())
                } else {
                    userSessionRepository.deleteAllUserSessions(rotatedData.getUserId())

                    userRepository.updateUser(
                        userId = rotatedData.getUserId(),
                        status = UpdateField.Set(UserAccountStatus.SECURITY_HOLD)
                    )

                    redisManager.delete(buildRotatedRefreshKey(refreshTokenHash.value))

                    auditLogger.log(
                        actorId = rotatedData.getUserId().asHexDashString(),
                        actorType = AuditActorType.USER,
                        action = UserAuditActionType.REFRESH_TOKEN_REUSE_DETECTED,
                        resource = UserAuditResourceType.USER,
                        resourceId = rotatedData.getUserId().asHexDashString(),
                        status = AuditStatus.FAILED,
                        metadata = setOf(
                            AuditEventMetadata(
                                key = UserAuditMetadataKey.USER_ID,
                                value = rotatedData.getUserId().asHexDashString()
                            )
                        )
                    )

                    return@dbQuery AppResult.Error(UserError.InvalidRefreshToken())
                }
            }
        }

        AppResult.Error(UserError.InvalidRefreshToken())
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

    private fun buildRotatedRefreshKey(hashValue: String): String = "auth:rotated_refresh:$hashValue"
}