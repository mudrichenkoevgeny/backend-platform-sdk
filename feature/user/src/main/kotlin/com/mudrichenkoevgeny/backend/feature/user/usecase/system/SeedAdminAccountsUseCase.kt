package io.github.mudrichenkoevgeny.backend.feature.user.usecase.system

import io.github.mudrichenkoevgeny.backend.core.common.config.seed.AdminAccount
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.config.model.UserConfig
import io.github.mudrichenkoevgeny.backend.feature.user.enums.UserAuthProvider
import io.github.mudrichenkoevgeny.backend.feature.user.enums.UserRole
import io.github.mudrichenkoevgeny.backend.feature.user.manager.auth.AuthManager
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
    suspend fun execute(
        adminAccounts: List<AdminAccount> = userConfig.adminAccountsList
    ): AppResult<Unit> = coroutineScope {
        val resultsList = adminAccounts.map { adminAccount ->
            async {
                authManager.getOrCreateUserIdentifier(
                    userAuthProvider = UserAuthProvider.EMAIL,
                    identifier = adminAccount.email,
                    password = adminAccount.password,
                    userRole = UserRole.ADMIN
                )
            }
        }.awaitAll()

        resultsList.filterIsInstance<AppResult.Error>().firstOrNull()?.let { errorResult ->
            return@coroutineScope AppResult.Error(errorResult.error)
        }

        AppResult.Success(Unit)
    }
}