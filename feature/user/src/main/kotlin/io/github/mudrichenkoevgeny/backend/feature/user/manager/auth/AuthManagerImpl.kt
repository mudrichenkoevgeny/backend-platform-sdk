package io.github.mudrichenkoevgeny.backend.feature.user.manager.auth

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.mapNotNullOrError
import io.github.mudrichenkoevgeny.backend.core.common.result.mapSuccess
import io.github.mudrichenkoevgeny.backend.core.database.util.dbQuery
import io.github.mudrichenkoevgeny.backend.core.security.error.model.SecurityError
import io.github.mudrichenkoevgeny.backend.core.security.passwordhasher.PasswordHasher
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.identifier.IdentifierManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.manager.WebSocketManager
import io.github.mudrichenkoevgeny.backend.feature.user.provider.authsettings.AuthSettingsProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientInfo
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.permission.PermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.model.websocket.SocketFrame
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.data.AuthData
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifier
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierInternal
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserDetails
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.mapper.identifier.toUserIdentifier
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.contract.UserWebSocketEventTypes
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock

/**
 * Default [AuthManager] implementation orchestrating complex authentication flows.
 *
 * Coordinates [UserManager], [IdentifierManager], and [SessionManager] to handle user lifecycle,
 * credential verification (via [PasswordHasher]), and session establishment.
 *
 * Manages security constraints such as active session limits, multi-identifier policies,
 * and account status enforcement (e.g., banning/holds).
 */
@Singleton
class AuthManagerImpl @Inject constructor(
    private val userManager: UserManager,
    private val identifierManager: IdentifierManager,
    private val sessionManager: SessionManager,
    private val passwordHasher: PasswordHasher,
    private val authSettingsProvider: AuthSettingsProvider,
    private val webSocketManager: WebSocketManager
) : AuthManager {

    override suspend fun authenticateOrCreateUser(
        clientInfo: ClientInfo,
        userAuthProvider: UserAuthProvider,
        identifier: String,
        password: String?,
        externalProviderEmail: String?,
        roleForUserCreation: UserRole,
        accountStatusForUserCreation: UserAccountStatus,
        authorityLevelForUserCreation: Int,
        permissionCodesForUserCreation: Set<PermissionCode>
    ): AppResult<AuthData> = dbQuery {
        val getOrCreateUserIdentifierResult = getOrCreateIdentifierForUnauthorizedUser(
            userAuthProvider = userAuthProvider,
            identifier = identifier,
            password = password,
            externalProviderEmail = externalProviderEmail,
            roleForUserCreation = roleForUserCreation,
            accountStatusForUserCreation = accountStatusForUserCreation,
            authorityLevelForUserCreation = authorityLevelForUserCreation,
            permissionCodesForUserCreation = permissionCodesForUserCreation
        )
        val userIdentifier = when (getOrCreateUserIdentifierResult) {
            is AppResult.Error -> return@dbQuery getOrCreateUserIdentifierResult
            is AppResult.Success -> getOrCreateUserIdentifierResult.data
        }

        provideAuthData(
            userIdentifier = userIdentifier,
            clientInfo = clientInfo
        )
    }

    override suspend fun authenticateExistingUser(
        clientInfo: ClientInfo,
        userAuthProvider: UserAuthProvider,
        identifier: String,
        password: String?,
        externalProviderEmail: String?
    ): AppResult<AuthData> = dbQuery {
        val resolvedUserIdResult = resolveUserIdByAuthData(
            provider = userAuthProvider,
            identifier = identifier,
            externalProviderEmail = externalProviderEmail
        ).mapNotNullOrError(UserError.UserNotFound())

        val userId = when (resolvedUserIdResult) {
            is AppResult.Success -> resolvedUserIdResult.data
            is AppResult.Error -> return@dbQuery resolvedUserIdResult
        }

        val userIdentifierResult = getIdentifierByUserId(
            userId = userId,
            provider = userAuthProvider,
            identifier = identifier,
            password = password
        )

        val userIdentifier = when (userIdentifierResult) {
            is AppResult.Success -> userIdentifierResult.data
            is AppResult.Error -> return@dbQuery userIdentifierResult
        }

        provideAuthData(
            userIdentifier = userIdentifier,
            clientInfo = clientInfo
        )
    }

    override suspend fun createUserAndIdentifier(
        userAuthProvider: UserAuthProvider,
        identifier: String,
        password: String?,
        externalProviderEmail: String?,
        roleForUserCreation: UserRole,
        accountStatusForUserCreation: UserAccountStatus,
        authorityLevelForUserCreation: Int,
        permissionCodesForUserCreation: Set<PermissionCode>
    ): AppResult<UserDetails> = dbQuery {
        val getUserIdentifierResult = identifierManager.getUserIdentifierInternalByProvider(
            userAuthProvider = userAuthProvider,
            identifier = identifier
        )

        val existingIdentifier = when (getUserIdentifierResult) {
            is AppResult.Success -> getUserIdentifierResult.data
            is AppResult.Error -> return@dbQuery getUserIdentifierResult
        }

        if (existingIdentifier != null) {
            return@dbQuery AppResult.Error(UserError.CannotCreateUserIdentifier())
        }

        val userResult = userManager.getOrCreateUser(
            userId = null,
            role = roleForUserCreation,
            accountStatus = accountStatusForUserCreation,
            authorityLevel = authorityLevelForUserCreation,
            permissions = permissionCodesForUserCreation
        )
        val user = when (userResult) {
            is AppResult.Success -> userResult.data
            is AppResult.Error -> return@dbQuery userResult
        }

        val createUserIdentifierResult = identifierManager.createUserIdentifier(
            userId = user.id,
            userAuthProvider = userAuthProvider,
            identifier = identifier,
            password = password
        ).mapSuccess { userIdentifierInternal ->
            userIdentifierInternal.toUserIdentifier()
        }

        when (createUserIdentifierResult) {
            is AppResult.Error -> createUserIdentifierResult
            is AppResult.Success -> AppResult.Success(user)
        }
    }

    override suspend fun createIdentifierForAuthorizedUser(
        userId: UserId,
        userAuthProvider: UserAuthProvider,
        identifier: String,
        password: String?,
        externalProviderEmail: String?
    ): AppResult<UserIdentifier> = dbQuery {
         val getUserIdentifiersResult = identifierManager.getUserIdentifiersByUserId(userId)
        val userIdentifiersList = when (getUserIdentifiersResult) {
            is AppResult.Success -> getUserIdentifiersResult.data
            is AppResult.Error -> return@dbQuery getUserIdentifiersResult
        }

        val checkCanAddNewIdentifierResult = checkCanAddNewIdentifier(
            newIdentifierUserAuthProvider = userAuthProvider,
            currentUserIdentifiers = userIdentifiersList
        )
        if (checkCanAddNewIdentifierResult is AppResult.Error) {
            return@dbQuery checkCanAddNewIdentifierResult
        }

        val getUserIdentifierResult = identifierManager.getUserIdentifierInternalByProvider(
            userAuthProvider = userAuthProvider,
            identifier = identifier
        )

        val existingIdentifier = when (getUserIdentifierResult) {
            is AppResult.Success -> getUserIdentifierResult.data
            is AppResult.Error -> return@dbQuery getUserIdentifierResult
        }

        if (existingIdentifier != null) {
            return@dbQuery AppResult.Error(UserError.CannotCreateUserIdentifier())
        }

        identifierManager.createUserIdentifier(
            userId = userId,
            userAuthProvider = userAuthProvider,
            identifier = identifier,
            password = password
        ).mapSuccess { userIdentifierInternal ->
            userIdentifierInternal.toUserIdentifier()
        }
    }

    override suspend fun completeMfaAuthentication(
        userId: UserId,
        userIdentifierId: UserIdentifierId,
        clientInfo: ClientInfo
    ): AppResult<AuthData> = dbQuery {
        val userIdentifierResult = identifierManager.getUserIdentifierByIdForSystem(userIdentifierId)
            .mapNotNullOrError(SecurityError.InvalidMfaToken())

        val userIdentifier = when (userIdentifierResult) {
            is AppResult.Success -> userIdentifierResult.data
            is AppResult.Error -> return@dbQuery userIdentifierResult
        }

        if (userIdentifier.userId != userId) {
            return@dbQuery AppResult.Error(UserError.InvalidCredentials())
        }

        provideAuthData(
            userIdentifier = userIdentifier,
            clientInfo = clientInfo
        )
    }

    private suspend fun getOrCreateIdentifierForUnauthorizedUser(
        userAuthProvider: UserAuthProvider,
        identifier: String,
        password: String?,
        externalProviderEmail: String?,
        roleForUserCreation: UserRole,
        accountStatusForUserCreation: UserAccountStatus,
        authorityLevelForUserCreation: Int,
        permissionCodesForUserCreation: Set<PermissionCode>
    ): AppResult<UserIdentifierInternal> {
        val resolvedUserIdResult = resolveUserIdByAuthData(
            provider = userAuthProvider,
            identifier = identifier,
            externalProviderEmail = externalProviderEmail
        )

        val resolvedUserId = when (resolvedUserIdResult) {
            is AppResult.Success -> resolvedUserIdResult.data
            is AppResult.Error -> return resolvedUserIdResult
        }

        val userId = if (resolvedUserId != null) {
            resolvedUserId
        } else {
            val userResult = userManager.getOrCreateUser(
                userId = null,
                role = roleForUserCreation,
                accountStatus = accountStatusForUserCreation,
                authorityLevel = authorityLevelForUserCreation,
                permissions = permissionCodesForUserCreation
            )
            when (userResult) {
                is AppResult.Success -> userResult.data.id
                is AppResult.Error -> return userResult
            }
        }

        return getOrCreateIdentifierByUserId(
            userId = userId,
            provider = userAuthProvider,
            identifier = identifier,
            password = password
        )
    }

    private suspend fun resolveUserIdByAuthData(
        provider: UserAuthProvider,
        identifier: String,
        externalProviderEmail: String?
    ): AppResult<UserId?> {
        val getUserIdentifierResult = identifierManager.getUserIdentifierInternalByProvider(
            userAuthProvider = provider,
            identifier = identifier
        )

        val existingIdentifier = when (getUserIdentifierResult) {
            is AppResult.Success -> getUserIdentifierResult.data
            is AppResult.Error -> return getUserIdentifierResult
        }

        if (existingIdentifier != null) {
            return AppResult.Success(existingIdentifier.userId)
        }

        if (externalProviderEmail != null) {
            val getExistingEmailIdentifierResult = identifierManager.getUserIdentifierInternalByProvider(
                userAuthProvider = UserAuthProvider.EMAIL,
                identifier = externalProviderEmail
            )

            val emailIdentifier = when (getExistingEmailIdentifierResult) {
                is AppResult.Success -> getExistingEmailIdentifierResult.data
                is AppResult.Error -> return getExistingEmailIdentifierResult
            }

            if (emailIdentifier != null) {
                return AppResult.Success(emailIdentifier.userId)
            }
        }

        return AppResult.Success(null)
    }

    private suspend fun getOrCreateIdentifierByUserId(
        userId: UserId,
        provider: UserAuthProvider,
        identifier: String,
        password: String?
    ): AppResult<UserIdentifierInternal> {
        val getUserIdentifierResult = identifierManager.getUserIdentifierInternalByProvider(
            userAuthProvider = provider,
            identifier = identifier
        )

        val existingIdentifier = when (getUserIdentifierResult) {
            is AppResult.Success -> getUserIdentifierResult.data
            is AppResult.Error -> return getUserIdentifierResult
        }

        if (existingIdentifier != null) {
            return checkCredentials(
                userId = userId,
                userIdentifierInternal = existingIdentifier,
                password = password
            )
        }

        return identifierManager.createUserIdentifier(
            userId = userId,
            userAuthProvider = provider,
            identifier = identifier,
            password = password
        )
    }

    private suspend fun getIdentifierByUserId(
        userId: UserId,
        provider: UserAuthProvider,
        identifier: String,
        password: String?
    ): AppResult<UserIdentifierInternal> {
        val getUserIdentifierResult = identifierManager.getUserIdentifierInternalByProvider(
            userAuthProvider = provider,
            identifier = identifier
        ).mapNotNullOrError(UserError.InvalidCredentials())

        val existingIdentifier = when (getUserIdentifierResult) {
            is AppResult.Success -> getUserIdentifierResult.data
            is AppResult.Error -> return getUserIdentifierResult
        }

        return checkCredentials(
            userId = userId,
            userIdentifierInternal = existingIdentifier,
            password = password
        )
    }

    private fun checkCredentials(
        userId: UserId,
        userIdentifierInternal: UserIdentifierInternal,
        password: String?
    ): AppResult<UserIdentifierInternal> {
        val isNeedToCheckPassword = userIdentifierInternal.userAuthProvider == UserAuthProvider.EMAIL
        if (userId != userIdentifierInternal.userId) {
            if (isNeedToCheckPassword) {
                passwordHasher.isPasswordValidFakeCheck(password)
            }
            return AppResult.Error(UserError.InvalidCredentials())
        }
        return if (isNeedToCheckPassword) {
            val isPasswordValidResult = passwordHasher.isPasswordValid(
                password = password,
                passwordHash = userIdentifierInternal.passwordHash
            )
            val isPasswordValid = when (isPasswordValidResult) {
                is AppResult.Success -> isPasswordValidResult.data
                is AppResult.Error -> {
                    return isPasswordValidResult
                }
            }
            if (!isPasswordValid) {
                AppResult.Error(UserError.InvalidCredentials())
            } else {
                AppResult.Success(userIdentifierInternal)
            }
        } else {
            AppResult.Success(userIdentifierInternal)
        }
    }

    private suspend fun provideAuthData(
        userIdentifier: UserIdentifierInternal,
        clientInfo: ClientInfo,
        existingUser: UserDetails? = null
    ): AppResult<AuthData> {
        val user = if (existingUser != null && existingUser.id == userIdentifier.userId) {
            existingUser
        } else {
            val userResult = userManager.getUserByIdForSelf(userIdentifier.userId)
                .mapNotNullOrError(UserError.UserNotFound())

            when (userResult) {
                is AppResult.Success -> userResult.data
                is AppResult.Error -> return userResult
            }
        }

        if (user.accountStatus == UserAccountStatus.BANNED || user.accountStatus == UserAccountStatus.SECURITY_HOLD) {
            return AppResult.Error(UserError.UserBlocked(user.id))
        }

        val userSessionsResult = sessionManager.getAllUserSessions(user.id)
        val userSessions = when (userSessionsResult) {
            is AppResult.Success -> userSessionsResult.data
            is AppResult.Error -> return userSessionsResult
        }

        if (userSessions.size >= authSettingsProvider.getMaxActiveSessions()) {
            val deletedSessionResult = sessionManager.deleteLeastRecentlyUsedUserSession(user.id)
            if (deletedSessionResult is AppResult.Success) {
                webSocketManager.sendMessageToUserSession(
                    userSessionId = deletedSessionResult.data,
                    frame = SocketFrame(
                        type = UserWebSocketEventTypes.SESSION_DELETED,
                        timestamp = Clock.System.now().toEpochMilliseconds()
                    )
                )
            }
        }

        val sessionTokenResult = sessionManager.createSession(
            userId = user.id,
            userRole = user.role,
            identifier = userIdentifier.identifier,
            identifierId = userIdentifier.id,
            identifierAuthProvider = userIdentifier.userAuthProvider,
            clientInfo = clientInfo,
            lastReauthenticatedAt = Clock.System.now()
        )
        val sessionToken = when (sessionTokenResult) {
            is AppResult.Success -> sessionTokenResult.data
            is AppResult.Error -> return sessionTokenResult
        }

        return AppResult.Success(
            AuthData(
                userDetails = user,
                sessionToken = sessionToken
            )
        )
    }

    fun checkCanAddNewIdentifier(
        newIdentifierUserAuthProvider: UserAuthProvider,
        currentUserIdentifiers: List<UserIdentifierInternal>
    ): AppResult<Unit> {
        val maxTotalNumberOfIdentifiers = authSettingsProvider.getMaxTotalIdentifiers()
        if (currentUserIdentifiers.size >= maxTotalNumberOfIdentifiers) {
            return AppResult.Error(
                UserError.TotalUserIdentifiersLimitReached(
                    maxNumberOfIdentifiers = maxTotalNumberOfIdentifiers
                )
            )
        }
        val currentUserIdentifiersWithSameType = currentUserIdentifiers.filter { identifier ->
            identifier.userAuthProvider == newIdentifierUserAuthProvider
        }
        when (newIdentifierUserAuthProvider) {
            UserAuthProvider.EMAIL -> {
                val maxNumberOfIdentifiers = authSettingsProvider.getMaxEmailIdentifiers()
                if (currentUserIdentifiersWithSameType.size >= maxNumberOfIdentifiers) {
                    return AppResult.Error(
                        UserError.UserIdentifierLimitReached(
                            maxNumberOfIdentifiers = maxNumberOfIdentifiers,
                            userAuthProvider = newIdentifierUserAuthProvider
                        )
                    )
                }
            }
            UserAuthProvider.PHONE -> {
                val maxNumberOfIdentifiers = authSettingsProvider.getMaxPhoneIdentifiers()
                if (currentUserIdentifiersWithSameType.size >= maxNumberOfIdentifiers) {
                    return AppResult.Error(
                        UserError.UserIdentifierLimitReached(
                            maxNumberOfIdentifiers = maxNumberOfIdentifiers,
                            userAuthProvider = newIdentifierUserAuthProvider
                        )
                    )
                }
            }
            UserAuthProvider.GOOGLE -> {
                val maxNumberOfIdentifiers = authSettingsProvider.getMaxIdentifiersPerExternalProvider()
                if (currentUserIdentifiersWithSameType.size >= maxNumberOfIdentifiers) {
                    return AppResult.Error(
                        UserError.UserIdentifierLimitReached(
                            maxNumberOfIdentifiers = maxNumberOfIdentifiers,
                            userAuthProvider = newIdentifierUserAuthProvider
                        )
                    )
                }
            }
            UserAuthProvider.APPLE -> {
                val maxNumberOfIdentifiers = authSettingsProvider.getMaxIdentifiersPerExternalProvider()
                if (currentUserIdentifiersWithSameType.size >= maxNumberOfIdentifiers) {
                    return AppResult.Error(
                        UserError.UserIdentifierLimitReached(
                            maxNumberOfIdentifiers = maxNumberOfIdentifiers,
                            userAuthProvider = newIdentifierUserAuthProvider
                        )
                    )
                }
            }
        }
        return AppResult.Success(Unit)
    }
}