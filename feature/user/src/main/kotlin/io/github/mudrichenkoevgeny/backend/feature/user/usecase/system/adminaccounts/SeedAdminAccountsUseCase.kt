package io.github.mudrichenkoevgeny.backend.feature.user.usecase.system.adminaccounts

import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.config.model.UserConfig
import io.github.mudrichenkoevgeny.backend.feature.user.config.seed.AdminAccount
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.user.CreateUserParams
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

/**
 * System use case for seeding initial administrator accounts into persistent storage.
 *
 * Runs during application startup to bootstrap initial administrative accounts with root authority levels
 * and configured permission sets if they do not already exist.
 */
@Singleton
public class SeedAdminAccountsUseCase @Inject constructor(
    private val userConfig: UserConfig,
    private val authManager: AuthManager
) {
    /**
     * Seeds administrative accounts defined in environment configuration into persistent storage.
     *
     * **Allowed Account Statuses:** Any (System initialization).
     *
     * **Security:**
     * - System-level bootstrap component; executed on startup without user context.
     * - Configures seed accounts with [UserRole.ADMIN], [UserAccountStatus.ACTIVE], and top authority level.
     *
     * **Workflow:**
     * 1. Iterates over [adminAccounts] configured in environment variables or configuration files.
     * 2. Calls [AuthManager.createUserAndIdentifier] for each account in parallel via [coroutineScope].
     * 3. Ignores expected [UserError.CannotCreateUserIdentifier] errors if admin accounts already exist.
     * 4. Returns [AppResult.Success] when all admin accounts exist or are created.
     *
     * @param adminAccounts The list of administrative accounts to seed.
     * @param permissionCodesForUserCreation The set of permissions assigned to new admin accounts.
     * @return [AppResult.Success] on successful seeding, or an [AppError] on critical failure.
     */
    public suspend operator fun invoke(
        adminAccounts: List<AdminAccount> = userConfig.adminAccountsList,
        permissionCodesForUserCreation: Set<PermissionCode>
    ): AppResult<Unit> = coroutineScope {
        val resultsList = adminAccounts.map { adminAccount ->
            async {
                authManager.createUserAndIdentifier(
                    userAuthProvider = UserAuthProvider.EMAIL,
                    createUserParams = CreateUserParams(
                        email = adminAccount.email,
                        password = adminAccount.password,
                        role = UserRole.ADMIN,
                        accountStatus = UserAccountStatus.ACTIVE,
                        authorityLevel = 100,
                        permissions = permissionCodesForUserCreation,
                        nickname = adminAccount.nickname,
                        displayName = adminAccount.displayName
                    )
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
