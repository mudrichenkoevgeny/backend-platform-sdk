package io.github.mudrichenkoevgeny.backend.feature.user.manager.auth

import io.github.mudrichenkoevgeny.backend.core.common.network.request.model.ClientInfo
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.model.useridentifier.UserIdentifier
import io.github.mudrichenkoevgeny.backend.feature.user.model.auth.AuthData
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId

/**
 * Orchestrates authentication operations for the user feature.
 *
 * Coordinates user loading, identifier creation, and session creation to produce [AuthData] for clients.
 */
interface AuthManager {
    /**
     * Builds [AuthData] for a successfully authenticated identifier.
     *
     * Typically, loads the user, validates access against [allowedRoles], checks account status, and
     * creates a new refresh session bound to the provided [clientInfo].
     *
     * @param userIdentifier authenticated identifier
     * @param clientInfo client metadata used to bind the created session
     * @param allowedRoles roles allowed to authenticate in this context
     * @return auth data or an error
     */
    suspend fun provideAuthData(
        userIdentifier: UserIdentifier,
        clientInfo: ClientInfo,
        allowedRoles: Set<UserRole>
    ): AppResult<AuthData>

    /**
     * Creates a new identifier for an existing user, failing if such identifier already exists.
     *
     * @param userId user id
     * @param userAuthProvider provider type
     * @param identifier identifier value (email/phone/external subject)
     * @param password optional password for password-based providers
     * @return created identifier or an error
     */
    suspend fun createUserIdentifier(
        userId: UserId,
        userAuthProvider: UserAuthProvider,
        identifier: String,
        password: String? = null
    ): AppResult<UserIdentifier>

    /**
     * Loads an existing identifier or creates a new one.
     *
     * When the identifier does not exist, creates (or loads) the owning user and then creates the identifier.
     *
     * @param userAuthProvider provider type
     * @param identifier identifier value
     * @param password optional password for password-based providers
     * @param userId optional existing user id to attach to
     * @param userRole role used when creating a new user
     * @return identifier or an error
     */
    suspend fun getOrCreateUserIdentifier(
        userAuthProvider: UserAuthProvider,
        identifier: String,
        password: String? = null,
        userId: UserId? = null,
        userRole: UserRole = UserRole.USER
    ): AppResult<UserIdentifier>
}