package io.github.mudrichenkoevgeny.backend.feature.user.manager.session

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientInfo
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientType
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.SortOrder
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.permission.PermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserSortValues
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.DeletedSessions
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionInternal
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionPrivate
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionSummary
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.token.RefreshToken
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.token.SessionToken
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import kotlin.time.Instant

/**
 * Manages refresh sessions for the user feature.
 *
 * Responsible for issuing session tokens, persisting refresh sessions, and performing refresh/delete operations.
 */
interface SessionManager {
    /**
     * Creates a new session for the given user and identifier.
     *
     * @param userId The unique identifier of the user.
     * @param userRole The role of the user to be encoded in the session data.
     * @param identifier The literal identifier value (e.g., email or phone) used for this session.
     * @param identifierId The ID of the specific identifier used to authenticate.
     * @param identifierDisplayName The display name of the identifier.
     * @param identifierAuthProvider The authentication provider used to authenticate.
     * @param clientInfo Client metadata (device, IP, etc.) to bind to the created session.
     * @param lastReauthenticatedAt Timestamp used to track re-authentication requirements.
     * @param checkNewDevice Whether to check for new device login.
     * @return [AppResult.Success] with [SessionToken] or an error.
     */
    suspend fun createSession(
        userId: UserId,
        userRole: UserRole,
        identifier: String,
        identifierId: UserIdentifierId,
        identifierDisplayName: String = identifier,
        identifierAuthProvider: UserAuthProvider,
        clientInfo: ClientInfo,
        lastReauthenticatedAt: Instant,
        checkNewDevice: Boolean = true
    ): AppResult<SessionToken>

    /**
     * Refreshes a session token using a refresh token.
     *
     * @param refreshToken The refresh token used to authorize the renewal.
     * @param clientInfo Updated client metadata used for session validation.
     * @param allowedRoles Allowed roles for the session user.
     * @param allowedAccountStatuses Allowed account statuses for the session user.
     * @return [AppResult.Success] with a new [SessionToken] or an error.
     */
    suspend fun refreshSession(
        refreshToken: RefreshToken,
        clientInfo: ClientInfo,
        allowedRoles: Set<UserRole> = UserRole.entries.toSet(),
        allowedAccountStatuses: Set<UserAccountStatus> = UserAccountStatus.entries.toSet()
    ): AppResult<SessionToken>

    /**
     * Updates the "last accessed" timestamp for a session.
     *
     * @param userSessionId The ID of the session to update.
     * @return [AppResult.Success] containing [Unit] or an error.
     */
    suspend fun updateLastAccessed(userSessionId: UserSessionId): AppResult<Unit>

    /**
     * Updates the "last reauthenticated" timestamp for a session.
     *
     * @param userSessionId The ID of the session to update.
     * @return [AppResult.Success] containing [Unit] or an error.
     */
    suspend fun updateLastReauthenticated(userSessionId: UserSessionId): AppResult<Unit>

    /**
     * Deletes a session by its ID.
     *
     * @param userSessionId The ID of the session to delete.
     * @return [AppResult.Success] containing [Unit] or an error.
     */
    suspend fun deleteSessionById(userSessionId: UserSessionId): AppResult<Unit>

    /**
     * Deletes all sessions for a specific user.
     *
     * @param userId The unique identifier of the user.
     * @return [AppResult.Success] containing [Unit] or an error.
     */
    suspend fun deleteAllUserSessions(userId: UserId): AppResult<Unit>

    /**
     * Deletes all sessions for a user except the one specified by [userSessionId].
     *
     * Used for "logout from other devices" scenarios.
     *
     * @param userId The unique identifier of the user.
     * @param userSessionId The ID of the session that should remain active.
     * @return [AppResult.Success] with [DeletedSessions] summary or an error.
     */
    suspend fun deleteAllUserSessionsExceptOne(
        userId: UserId,
        userSessionId: UserSessionId
    ): AppResult<DeletedSessions>

    /**
     * Deletes the least recently used (LRU) session for a specific user.
     * Used to free up space when the user exceeds the maximum allowed number of concurrent sessions.
     *
     * @param userId user id
     * @return [AppResult.Success] with the ID of the deleted session or an error.
     */
    suspend fun deleteLeastRecentlyUsedUserSession(userId: UserId): AppResult<UserSessionId>

    /**
     * Loads a session by its ID for internal system use.
     *
     * Returns full technical session data including internal IDs and metadata.
     *
     * @param userSessionId The ID of the session to retrieve.
     * @return [AppResult.Success] with [UserSessionInternal] if found, null otherwise, or an error.
     */
    suspend fun getUserSessionInternal(userSessionId: UserSessionId): AppResult<UserSessionInternal?>

    /**
     * Loads a session by its ID for the current user's self-service access.
     *
     * @param userSessionId The ID of the session to retrieve.
     * @return [AppResult.Success] with [UserSessionPrivate] if found, null otherwise, or an error.
     */
    suspend fun getUserSessionPrivate(userSessionId: UserSessionId): AppResult<UserSessionPrivate?>

    /**
     * Loads all sessions for a user as internal models.
     *
     * @param userId The unique identifier of the user.
     * @return [AppResult.Success] with a list of [UserSessionInternal] or an error.
     */
    suspend fun getAllUserSessionsInternalList(userId: UserId): AppResult<List<UserSessionInternal>>

    /**
     * Loads sessions associated with a specific identifier ID, with an optional owner filter.
     *
     * @param userIdentifierId The ID of the identifier (e.g., specific email/phone entry).
     * @param userId Optional owner filter to ensure sessions belong to the specified user.
     * @return [AppResult.Success] with a list of [UserSessionInternal] or an error.
     */
    suspend fun getUserSessionsInternalListByIdentifierId(
        userIdentifierId: UserIdentifierId,
        userId: UserId? = null
    ): AppResult<List<UserSessionInternal>>

    /**
     * Loads a session by ID with permission-aware access checks for administrative use.
     *
     * [managementUserPermissionCodes] define visibility rules.
     *
     * @param userSessionId The ID of the session to retrieve.
     * @param managementUserId The ID of the manager/admin performing the request.
     * @param managementUserPermissionCodes Permissions used to derive access rules.
     * @return [AppResult.Success] with [UserSessionPrivate], null if not found, or [AppResult.Error].
     */
    suspend fun getUserSessionPrivate(
        userSessionId: UserSessionId,
        managementUserId: UserId,
        managementUserPermissionCodes: Set<PermissionCode>
    ): AppResult<UserSessionPrivate?>

    /**
     * Returns a paginated list of user session summaries for management purposes.
     *
     * Applies role-scoped visibility based on [managementUserPermissionCodes].
     *
     * @param managementUserPermissionCodes Caller permissions used to derive access rules.
     * @param pageParams Pagination settings (page index and size).
     * @param sortBy Field to sort the results by.
     * @param sortOrder Sorting direction (ASC/DESC).
     * @param userIds Optional filter for specific user IDs.
     * @param userRoles optional filters for specific user roles
     * @param identifiers Optional filter for identifier values (e.g., specific emails).
     * @param identifierIds Optional filter for specific user identifier IDs.
     * @param identifierAuthProviders Optional filter for authentication providers.
     * @param clientTypes Optional filter by client platform types.
     * @param userAgents Optional filter for client user-agent strings.
     * @param ipAddresses Optional filter for client IP addresses.
     * @param languages Optional filter for client language headers.
     * @param deviceIds Optional filter for unique hardware device IDs.
     * @param deviceNames Optional filter for human-readable device names.
     * @param appVersions Optional filter for application versions.
     * @param operationSystemVersions Optional filter for OS versions.
     * @return [AppResult.Success] with [PagedResult] of session summaries or an error.
     */
    suspend fun getUserSessionsSummaryPage(
        managementUserPermissionCodes: Set<PermissionCode>,
        pageParams: PageParams,
        sortBy: UserSortValues.UserSessionSortBy = UserSortValues.UserSessionSortBy.CREATED_AT,
        sortOrder: SortOrder = SortOrder.DESC,
        userIds: List<UserId> = emptyList(),
        userRoles: List<UserRole> = emptyList(),
        identifiers: List<String> = emptyList(),
        identifierIds: List<UserIdentifierId> = emptyList(),
        identifierAuthProviders: List<UserAuthProvider> = emptyList(),
        clientTypes: List<ClientType> = emptyList(),
        userAgents: List<String> = emptyList(),
        ipAddresses: List<String> = emptyList(),
        languages: List<String> = emptyList(),
        deviceIds: List<String> = emptyList(),
        deviceNames: List<String> = emptyList(),
        appVersions: List<String> = emptyList(),
        operationSystemVersions: List<String> = emptyList()
    ): AppResult<PagedResult<UserSessionSummary>>

    /**
     * Returns a paginated list of session summaries belonging to the current user.
     *
     * Designed for "Self" service scenarios.
     *
     * @param userId The ID of the user requesting their own sessions.
     * @param pageParams Pagination settings (page index and size).
     * @param sortBy Field to sort the results by.
     * @param sortOrder Sorting direction.
     * @param identifiers Optional filter for identifier values associated with the session.
     * @param identifierIds Optional filter for specific user identifier IDs.
     * @param identifierAuthProviders Optional filter for authentication providers.
     * @param clientTypes Optional filter by client platform types.
     * @param userAgents Optional filter for client user-agent strings.
     * @param ipAddresses Optional filter for client IP addresses.
     * @param languages Optional filter for client language headers.
     * @param deviceIds Optional filter for unique hardware device IDs.
     * @param deviceNames Optional filter for device names.
     * @param appVersions Optional filter for application versions.
     * @param operationSystemVersions Optional filter for OS versions.
     * @return [AppResult.Success] with [PagedResult] of the user's session summaries or an error.
     */
    suspend fun getUserSessionsSummaryPage(
        userId: UserId,
        pageParams: PageParams,
        sortBy: UserSortValues.UserSessionSortBy = UserSortValues.UserSessionSortBy.CREATED_AT,
        sortOrder: SortOrder = SortOrder.DESC,
        identifiers: List<String> = emptyList(),
        identifierIds: List<UserIdentifierId> = emptyList(),
        identifierAuthProviders: List<UserAuthProvider> = emptyList(),
        clientTypes: List<ClientType> = emptyList(),
        userAgents: List<String> = emptyList(),
        ipAddresses: List<String> = emptyList(),
        languages: List<String> = emptyList(),
        deviceIds: List<String> = emptyList(),
        deviceNames: List<String> = emptyList(),
        appVersions: List<String> = emptyList(),
        operationSystemVersions: List<String> = emptyList()
    ): AppResult<PagedResult<UserSessionSummary>>
}
