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
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserDetails
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SeedAdminAccountsUseCaseTest {

    private val userConfig = mockk<UserConfig>()
    private val authManager = mockk<AuthManager>()
    private val useCase = SeedAdminAccountsUseCase(userConfig, authManager)

    private val adminEmail = "admin@example.com"
    private val adminPassword = "password123"
    private val permissions = setOf(PermissionCode("all.access"))

    @Test
    fun `invoke - returns success when all accounts are created`() = runBlocking {
        val accounts = listOf(AdminAccount(adminEmail, adminPassword))

        coEvery {
            authManager.createUserAndIdentifier(
                userAuthProvider = UserAuthProvider.EMAIL,
                identifier = adminEmail,
                password = adminPassword,
                roleForUserCreation = UserRole.ADMIN,
                accountStatusForUserCreation = UserAccountStatus.ACTIVE,
                authorityLevelForUserCreation = 100,
                permissionCodesForUserCreation = permissions
            )
        } returns AppResult.Success(mockk<UserDetails>())

        val result = useCase(accounts, permissions)

        assertTrue(result is AppResult.Success)
    }

    @Test
    fun `invoke - ignores CannotCreateUserIdentifier error`() = runBlocking {
        val accounts = listOf(AdminAccount(adminEmail, adminPassword))
        val alreadyExistsError = UserError.CannotCreateUserIdentifier()

        coEvery {
            authManager.createUserAndIdentifier(
                any(), any(), any(), any(), any(), any(), any(), any()
            )
        } returns AppResult.Error(alreadyExistsError)

        val result = useCase(accounts, permissions)

        assertTrue(result is AppResult.Success)
    }

    @Test
    fun `invoke - returns first critical error encountered`() = runBlocking {
        val accounts = listOf(AdminAccount(adminEmail, adminPassword))
        val criticalError = UserError.UserForbidden()

        coEvery {
            authManager.createUserAndIdentifier(
                any(), any(), any(), any(), any(), any(), any(), any()
            )
        } returns AppResult.Error(criticalError)

        val result = useCase(accounts, permissions)

        assertTrue(result is AppResult.Error)
        assertEquals(criticalError, (result as AppResult.Error).error)
    }

    @Test
    fun `invoke - uses accounts from config when none provided`() = runBlocking {
        val configAccount = AdminAccount("config@admin.com", "secret")
        coEvery { userConfig.adminAccountsList } returns listOf(configAccount)

        coEvery {
            authManager.createUserAndIdentifier(
                userAuthProvider = any(),
                identifier = eq(configAccount.email),
                password = any(),
                externalProviderEmail = any(),
                roleForUserCreation = any(),
                accountStatusForUserCreation = any(),
                authorityLevelForUserCreation = any(),
                permissionCodesForUserCreation = any()
            )
        } returns AppResult.Success(mockk())

        val result = useCase(permissionCodesForUserCreation = permissions)

        assertTrue(result is AppResult.Success)
    }

    @Test
    fun `invoke - processes multiple accounts and stops at critical error`() = runBlocking {
        val accounts = listOf(
            AdminAccount("first@admin.com", "p1"),
            AdminAccount("second@admin.com", "p2")
        )
        val criticalError = UserError.UserForbidden()

        coEvery {
            authManager.createUserAndIdentifier(
                userAuthProvider = any(),
                identifier = eq("first@admin.com"),
                password = any(),
                externalProviderEmail = any(),
                roleForUserCreation = any(),
                accountStatusForUserCreation = any(),
                authorityLevelForUserCreation = any(),
                permissionCodesForUserCreation = any()
            )
        } returns AppResult.Error(criticalError)

        coEvery {
            authManager.createUserAndIdentifier(
                userAuthProvider = any(),
                identifier = eq("second@admin.com"),
                password = any(),
                externalProviderEmail = any(),
                roleForUserCreation = any(),
                accountStatusForUserCreation = any(),
                authorityLevelForUserCreation = any(),
                permissionCodesForUserCreation = any()
            )
        } returns AppResult.Success(mockk())

        val result = useCase(accounts, permissions)

        assertTrue(result is AppResult.Error)
        assertEquals(criticalError, (result as AppResult.Error).error)
    }
}