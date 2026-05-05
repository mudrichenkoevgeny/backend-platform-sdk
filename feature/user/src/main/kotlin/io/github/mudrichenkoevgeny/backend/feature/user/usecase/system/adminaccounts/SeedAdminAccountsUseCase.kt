package io.github.mudrichenkoevgeny.backend.feature.user.usecase.system.adminaccounts

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.config.model.UserConfig
import io.github.mudrichenkoevgeny.backend.feature.user.config.seed.AdminAccount
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.auth.AuthManager
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.permission.PermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SeedAdminAccountsUseCase @Inject constructor(
    private val userConfig: UserConfig,
    private val authManager: AuthManager
) {
    /**
     * Ensures that administrative accounts defined in the configuration are present in the system.
     * Usually executed during application startup to bootstrap initial access.
     *
     * **Workflow:**
     * 1. Iterates through the provided list of [AdminAccount] (defaulting to [UserConfig.adminAccountsList]).
     * 2. Executes [AuthManager.createUserAndIdentifier] for each account in parallel using a coroutine scope.
     * 3. Configures accounts with [UserRole.ADMIN], [UserAccountStatus.ACTIVE], and a high authority level (100).
     * 4. Filters out expected "already exists" errors ([UserError.CannotCreateUserIdentifier]).
     * 5. Returns the first critical error encountered, or [AppResult.Success] if all accounts are processed.
     *
     * @param adminAccounts The list of administrative accounts to seed.
     * @param permissionCodesForUserCreation The set of permissions to be assigned to the new admin users.
     * @return [AppResult] indicating whether the seeding process completed without critical failures.
     */
    suspend operator fun invoke(
        adminAccounts: List<AdminAccount> = userConfig.adminAccountsList,
        permissionCodesForUserCreation: Set<PermissionCode>
    ): AppResult<Unit> = coroutineScope {
        val resultsList = adminAccounts.map { adminAccount ->
            async {
                authManager.createUserAndIdentifier(
                    userAuthProvider = UserAuthProvider.EMAIL,
                    identifier = adminAccount.email,
                    password = adminAccount.password,
                    roleForUserCreation = UserRole.ADMIN,
                    accountStatusForUserCreation = UserAccountStatus.ACTIVE,
                    authorityLevelForUserCreation = 100,
                    permissionCodesForUserCreation = permissionCodesForUserCreation
                )
            }
        }.awaitAll()

        resultsList.filterIsInstance<AppResult.Error>()
            .firstOrNull { it.error !is UserError.CannotCreateUserIdentifier }?.let { errorResult ->
                return@coroutineScope AppResult.Error(errorResult.error)
            }

        AppResult.Success(Unit)
    }
}