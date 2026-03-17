package io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.model.user.User
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.UserRole
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall

/**
 * Configures request authentication and exposes authorization helpers for the user feature.
 *
 * Implementations are expected to integrate with Ktor authentication and to provide
 * a single place to enforce role/account-status rules when accessing protected resources.
 */
interface AuthenticationProvider {
    fun configureAuthentication(application: Application)

    /**
     * Authorizes a user based on their JWT token.
     *
     * @param call The current application call.
     * @param allowedRoles Roles that are allowed to access the resource. Default is all roles.
     * @param allowReadOnlyAccounts Whether users with READ_ONLY status are allowed. Default is true.
     * @param allowBannedAccounts Whether users with BANNED status are allowed. Default is false.
     * @return Result containing the authorized User or an error if authorization fails.
     */
    suspend fun requireUser(
        call: ApplicationCall,
        allowedRoles: Set<UserRole> = UserRole.entries.toSet(),
        allowReadOnlyAccounts: Boolean = true,
        allowBannedAccounts: Boolean = false
    ): AppResult<User>
}