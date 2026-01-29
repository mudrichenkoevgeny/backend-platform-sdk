package io.github.mudrichenkoevgeny.backend.feature.user.manager.auth

import io.github.mudrichenkoevgeny.backend.core.common.model.UserId
import io.github.mudrichenkoevgeny.backend.core.common.network.request.model.ClientInfo
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.mapNotNullOrError
import io.github.mudrichenkoevgeny.backend.core.database.util.dbQuery
import io.github.mudrichenkoevgeny.backend.feature.user.enums.UserAccountStatus
import io.github.mudrichenkoevgeny.backend.feature.user.enums.UserAuthProvider
import io.github.mudrichenkoevgeny.backend.feature.user.enums.UserRole
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.useridentifier.UserIdentifierManager
import io.github.mudrichenkoevgeny.backend.feature.user.model.useridentifier.UserIdentifier
import io.github.mudrichenkoevgeny.backend.feature.user.model.auth.AuthData
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthManagerImpl @Inject constructor(
    private val userManager: UserManager,
    private val userIdentifierManager: UserIdentifierManager,
    private val sessionManager: SessionManager
) : AuthManager {

    override suspend fun provideAuthData(
        userIdentifier: UserIdentifier,
        clientInfo: ClientInfo,
        allowedRoles: Set<UserRole>
    ): AppResult<AuthData> = dbQuery {
        val userResult = userManager.getUserById(
            userId = userIdentifier.userId
        ).mapNotNullOrError(UserError.UserNotFound())
        val user = when (userResult) {
            is AppResult.Success -> userResult.data
            is AppResult.Error -> return@dbQuery userResult
        }

        if (!allowedRoles.contains(user.role)) {
            return@dbQuery AppResult.Error(UserError.UserForbidden(user.id))
        }

        if (user.accountStatus == UserAccountStatus.BANNED) {
            return@dbQuery AppResult.Error(UserError.UserBlocked(user.id))
        }

        val sessionTokenResult = sessionManager.createSession(
            userId = user.id,
            userIdentifierId = userIdentifier.id,
            userIdentifierAuthProvider = userIdentifier.userAuthProvider,
            clientInfo = clientInfo,
            lastReauthenticatedAt = Instant.now()
        )
        val sessionToken = when (sessionTokenResult) {
            is AppResult.Success -> sessionTokenResult.data
            is AppResult.Error -> return@dbQuery sessionTokenResult
        }

        val userIdentifiersListResult = userIdentifierManager.getUserIdentifierListByUserId(userIdentifier.userId)

        val userIdentifiersList = if (userIdentifiersListResult is AppResult.Success) {
            userIdentifiersListResult.data
        } else {
            emptyList()
        }

        AppResult.Success(
            AuthData(
                user = user,
                userIdentifiersList = userIdentifiersList,
                sessionToken = sessionToken
            )
        )
    }

    override suspend fun createUserIdentifier(
        userId: UserId,
        userAuthProvider: UserAuthProvider,
        identifier: String,
        password: String?
    ): AppResult<UserIdentifier> = dbQuery {
        val userResult = userManager.getUserById(userId).mapNotNullOrError(UserError.UserNotFound())

        val user = when (userResult) {
            is AppResult.Success -> userResult.data
            is AppResult.Error -> return@dbQuery userResult
        }

        val userIdentifierResult = userIdentifierManager.getUserIdentifier(
            userAuthProvider = userAuthProvider,
            identifier = identifier
        )

        val userIdentifier: UserIdentifier? = when (userIdentifierResult) {
            is AppResult.Success -> userIdentifierResult.data
            is AppResult.Error -> return@dbQuery userIdentifierResult
        }

        if (userIdentifier != null) {
            return@dbQuery AppResult.Error(UserError.CannotCreateUserIdentifier())
        }

        userIdentifierManager.createUserIdentifier(
            userId = userId,
            userAuthProvider = userAuthProvider,
            identifier = identifier,
            password = password
        )
    }

    override suspend fun getOrCreateUserIdentifier(
        userAuthProvider: UserAuthProvider,
        identifier: String,
        password: String?,
        userId: UserId?,
        userRole: UserRole
    ): AppResult<UserIdentifier> {
        return dbQuery {
            val userIdentifierResult = userIdentifierManager.getUserIdentifier(
                userAuthProvider = userAuthProvider,
                identifier = identifier
            )

            val userIdentifier: UserIdentifier? = when (userIdentifierResult) {
                is AppResult.Success -> userIdentifierResult.data
                is AppResult.Error -> return@dbQuery userIdentifierResult
            }

            if (userIdentifier != null) {
                return@dbQuery AppResult.Success(userIdentifier)
            }

            val userResult = userManager.getOrCreateUser(
                userId = userId,
                role = userRole
            )

            val user = when (userResult) {
                is AppResult.Success -> userResult.data
                is AppResult.Error -> return@dbQuery userResult
            }

            userIdentifierManager.createUserIdentifier(
                userId = user.id,
                userAuthProvider = userAuthProvider,
                identifier = identifier,
                password = password
            )
        }
    }
}