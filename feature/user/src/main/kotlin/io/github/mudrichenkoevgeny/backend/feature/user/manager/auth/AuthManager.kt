package io.github.mudrichenkoevgeny.backend.feature.user.manager.auth

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientInfo
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.permission.PermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.data.AuthData
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifier
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserDetails
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId

/**
 * Orchestrates authentication operations for the user feature.
 *
 * Coordinates user loading, identifier creation, and session creation to produce [AuthData] for clients.
 * This manager serves as the high-level entry point for all authentication flows, including
 * registration, login, and multifactor authentication completion.
 */
interface AuthManager {
    /**
     * Authenticates a user or creates a new one if they don't exist.
     * * If the identifier is not found, a new user and corresponding identifier are created using the provided
     * default roles and permissions. If the user exists, it performs standard authentication.
     *
     * @param clientInfo Information about the client device and environment.
     * @param userAuthProvider The authentication method used (e.g., EMAIL, GOOGLE).
     * @param identifier The unique string for the provider (email, phone number, or social ID).
     * @param password The raw password (required for EMAIL provider).
     * @param externalProviderEmail Optional email from an external provider to link accounts if an email identifier already exists.
     * @param roleForUserCreation Role assigned if a new user is created. Defaults to [UserRole.USER].
     * @param accountStatusForUserCreation Initial status if a new user is created. Defaults to [UserAccountStatus.ACTIVE].
     * @param authorityLevelForUserCreation  Explicit authorityLevel for the new user. Defaults to 0
     * @param permissionCodesForUserCreation Set of initial permissions if a new user is created.
     * @return [AppResult] with [AuthData] containing user details and the session token.
     */
    suspend fun authenticateOrCreateUser(
        clientInfo: ClientInfo,
        userAuthProvider: UserAuthProvider,
        identifier: String,
        password: String? = null,
        externalProviderEmail: String? = null,
        roleForUserCreation: UserRole = UserRole.USER,
        accountStatusForUserCreation: UserAccountStatus = UserAccountStatus.ACTIVE,
        authorityLevelForUserCreation: Int = 0,
        permissionCodesForUserCreation: Set<PermissionCode> = setOf()
    ): AppResult<AuthData>

    /**
     * Authenticates an existing user and establishes a session.
     *
     * Performs credential verification (including password hashing check for email providers).
     * Fails if the user or identifier does not exist.
     *
     * @param clientInfo Information about the client device and environment.
     * @param userAuthProvider The authentication method used.
     * @param identifier The identifier string (email, phone, etc.).
     * @param password The raw password (required for EMAIL provider).
     * @param externalProviderEmail Optional email to aid in resolving an existing account.
     * @return [AppResult] with [AuthData].
     */
    suspend fun authenticateExistingUser(
        clientInfo: ClientInfo,
        userAuthProvider: UserAuthProvider,
        identifier: String,
        password: String? = null,
        externalProviderEmail: String? = null
    ): AppResult<AuthData>

    /**
     * Creates a new user and identifier without initiating a session.
     * * Primarily used for seeding data or administrative management. Fails if the identifier
     * is already registered.
     *
     * @param userAuthProvider The authentication method to be registered.
     * @param identifier The unique identifier string.
     * @param password Raw password to be hashed.
     * @param externalProviderEmail Optional email for account linking logic.
     * @param roleForUserCreation Explicit role for the new user.
     * @param accountStatusForUserCreation Explicit status for the new user.
     * @param authorityLevelForUserCreation  Explicit authorityLevel for the new user.
     * @param permissionCodesForUserCreation Initial permissions.
     * @return [AppResult] with the created [UserDetails].
     */
    suspend fun createUserAndIdentifier(
        userAuthProvider: UserAuthProvider,
        identifier: String,
        password: String? = null,
        externalProviderEmail: String? = null,
        roleForUserCreation: UserRole,
        accountStatusForUserCreation: UserAccountStatus,
        authorityLevelForUserCreation: Int,
        permissionCodesForUserCreation: Set<PermissionCode>
    ): AppResult<UserDetails>

    /**
     * Adds a new authentication identifier to an already authenticated user account.
     * * Validates that the new identifier is not already in use and that the user
     * has not exceeded the maximum number of identifiers allowed by system policy.
     *
     * @param userId ID of the user adding the new identifier.
     * @param userAuthProvider The new provider type to add.
     * @param identifier The identifier string for the new provider.
     * @param password Raw password (if adding an email provider).
     * @param externalProviderEmail Optional email from an external provider.
     * @return [AppResult] with the new [UserIdentifier].
     */
    suspend fun createIdentifierForAuthorizedUser(
        userId: UserId,
        userAuthProvider: UserAuthProvider,
        identifier: String,
        password: String? = null,
        externalProviderEmail: String? = null
    ): AppResult<UserIdentifier>

    /**
     * Completes the authentication process for a user who has successfully passed MFA.
     *
     * This method bypasses standard password verification and establishes a new session
     * based on the provided identifier ID. It ensures the account is not blocked
     * before generating the final authentication data.
     *
     * @param userId The unique identifier of the user.
     * @param userIdentifierId The ID of the specific identifier used for login.
     * @param clientInfo Information about the client device and environment.
     * @return [AppResult] containing [AuthData] or an error if the user is blocked or data is invalid.
     */
    suspend fun completeMfaAuthentication(
        userId: UserId,
        userIdentifierId: UserIdentifierId,
        clientInfo: ClientInfo
    ): AppResult<AuthData>
}