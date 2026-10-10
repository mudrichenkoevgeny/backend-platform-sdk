package io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.user.createTestUserInternal
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.permission.PermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserInternal
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall

class TestAuthenticationProvider : AuthenticationProvider {

    private var result: AppResult<UserInternal> = AppResult.Error(UserError.InvalidAccessToken())

    fun shouldReturnSuccess(user: UserInternal = createTestUserInternal()) {
        result = AppResult.Success(user)
    }

    fun shouldReturnError(error: AppResult.Error) {
        result = error
    }

    override fun configureAuthentication(application: Application) {
    }

    override suspend fun requireUser(
        call: ApplicationCall,
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>,
        requiredPermissions: Set<PermissionCode>
    ): AppResult<UserInternal> {
        return result
    }
}
