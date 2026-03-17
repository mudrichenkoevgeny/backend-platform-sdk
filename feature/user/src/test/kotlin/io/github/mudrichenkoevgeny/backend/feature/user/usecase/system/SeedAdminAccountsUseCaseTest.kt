package io.github.mudrichenkoevgeny.backend.feature.user.usecase.system

import io.github.mudrichenkoevgeny.backend.core.common.config.seed.AdminAccount
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.config.model.UserConfig
import io.github.mudrichenkoevgeny.backend.feature.user.manager.auth.AuthManager
import io.github.mudrichenkoevgeny.backend.feature.user.model.useridentifier.UserIdentifier
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.UserRole
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SeedAdminAccountsUseCaseTest {

    private val authManager = mockk<AuthManager>()
    private val userConfig = mockk<UserConfig>(relaxed = true)

    private val useCase = SeedAdminAccountsUseCase(
        userConfig = userConfig,
        authManager = authManager
    )

    @Test
    fun `execute returns success when no admin accounts`() = runBlocking {
        val result = useCase.execute(adminAccounts = emptyList())

        assertTrue(result is AppResult.Success)
    }

    @Test
    fun `execute returns success when all getOrCreateUserIdentifier succeed`() = runBlocking {
        val admins = listOf(
            AdminAccount(email = "admin1@example.com", password = "pass1"),
            AdminAccount(email = "admin2@example.com", password = "pass2")
        )
        val identifier = mockk<UserIdentifier>(relaxed = true)
        coEvery {
            authManager.getOrCreateUserIdentifier(
                userAuthProvider = UserAuthProvider.EMAIL,
                identifier = any(),
                password = any(),
                userId = any(),
                userRole = UserRole.ADMIN
            )
        } returns AppResult.Success(identifier)

        val result = useCase.execute(adminAccounts = admins)

        assertTrue(result is AppResult.Success)
    }

    @Test
    fun `execute returns first error when one getOrCreateUserIdentifier fails`() = runBlocking {
        val admins = listOf(
            AdminAccount(email = "admin1@example.com", password = "pass1"),
            AdminAccount(email = "admin2@example.com", password = "pass2")
        )
        val identifier = mockk<UserIdentifier>(relaxed = true)
        val error = UserError.InvalidAccessToken()
        var callCount = 0
        coEvery {
            authManager.getOrCreateUserIdentifier(
                userAuthProvider = UserAuthProvider.EMAIL,
                identifier = any(),
                password = any(),
                userId = any(),
                userRole = UserRole.ADMIN
            )
        } answers {
            callCount++
            if (callCount == 1) AppResult.Success(identifier) else AppResult.Error(error)
        }

        val result = useCase.execute(adminAccounts = admins)

        assertTrue(result is AppResult.Error)
        assertEquals(error, (result as AppResult.Error).error)
    }

    @Test
    fun `execute returns error when first getOrCreateUserIdentifier fails`() = runBlocking {
        val admins = listOf(AdminAccount(email = "admin@example.com", password = "pass"))
        val error = UserError.InvalidCredentials()
        coEvery {
            authManager.getOrCreateUserIdentifier(
                userAuthProvider = UserAuthProvider.EMAIL,
                identifier = any(),
                password = any(),
                userId = any(),
                userRole = UserRole.ADMIN
            )
        } returns AppResult.Error(error)

        val result = useCase.execute(adminAccounts = admins)

        assertTrue(result is AppResult.Error)
        assertEquals(error, (result as AppResult.Error).error)
    }
}
