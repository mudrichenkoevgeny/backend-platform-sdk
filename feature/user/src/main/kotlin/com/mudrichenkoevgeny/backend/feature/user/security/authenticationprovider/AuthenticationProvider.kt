package com.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider

import com.mudrichenkoevgeny.backend.core.common.result.AppResult
import com.mudrichenkoevgeny.backend.feature.user.enums.UserRole
import com.mudrichenkoevgeny.backend.feature.user.model.user.User
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall

interface AuthenticationProvider {
    fun configureAuthentication(application: Application)

    /**
     * Authorizes a user based on their JWT token.
     *
     * @param call The current application call.
     * @param checkToken If true, manually verifies the access token from the request headers.
     *                   Should be used only if there is no prior authentication
     *                   (e.g., no `authenticate("jwt")` applied). Default is false.
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