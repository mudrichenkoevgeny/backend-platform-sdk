package io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.permission.PermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserDetails
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall

/**
 * Configures request authentication and exposes authorization helpers for the user feature.
 *
 * Implementations integrate with Ktor (e.g. JWT), resolve the current principal from the call, load
 * [UserDetails] from persistence, and apply a consistent policy stack: **role**, **permissions** (when
 * requested), and **account status** (when constrained), mapping failures to [UserError] via
 * [AppResult].
 */
interface AuthenticationProvider {
    fun configureAuthentication(application: Application)

    /**
     * Resolves the authenticated user for [call] and enforces access rules before returning
     * [UserDetails].
     *
     * Typical order of checks (after a valid token / principal is present): load user by id from
     * storage; verify [allowedRoles]; verify [allowedAccountStatuses] when non-empty; verify
     * [requiredPermissions] when non-empty.
     *
     * @param call Current HTTP/WebSocket call (must carry credentials the implementation understands).
     * @param allowedRoles User must have one of these roles. **If empty, access is denied for everyone.**
     * @param allowedAccountStatuses User account status must be one of the listed values (**OR** semantics).
     * **If empty, access is denied for everyone.**
     * @param requiredPermissions When **empty** (default), permissions are not checked. When
     * **non-empty**, the user must hold **every** listed [PermissionCode] in
     * [UserDetails.permissionCodes] (**AND** semantics). Missing any required code yields the same
     * outcome as an insufficient role (e.g. forbidden).
     * @return [AppResult.Success] with the loaded user, or [AppResult.Error] (token, not found,
     * forbidden, blocked, read-only, security hold, pending deletion, etc.).
     */
    suspend fun requireUser(
        call: ApplicationCall,
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>,
        requiredPermissions: Set<PermissionCode> = setOf()
    ): AppResult<UserDetails>
}