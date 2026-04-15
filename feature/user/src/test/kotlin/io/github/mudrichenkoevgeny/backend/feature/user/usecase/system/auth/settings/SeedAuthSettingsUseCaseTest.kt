package io.github.mudrichenkoevgeny.backend.feature.user.usecase.system.auth.settings

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.provider.authsettings.AuthSettingsProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SeedAuthSettingsUseCaseTest {

    @Test
    fun `execute returns success when provider initialize succeeds`() = runTest {
        val provider = mockk<AuthSettingsProvider>()
        coEvery { provider.initialize() } returns AppResult.Success(Unit)
        val useCase = SeedAuthSettingsUseCase(provider)

        val result = useCase.execute()

        assertEquals(AppResult.Success(Unit), result)
        coVerify(exactly = 1) { provider.initialize() }
    }

    @Test
    fun `execute returns provider error when initialize fails`() = runTest {
        val provider = mockk<AuthSettingsProvider>()
        val err = CommonError.Internal(Throwable("db"))
        coEvery { provider.initialize() } returns AppResult.Error(err)
        val useCase = SeedAuthSettingsUseCase(provider)

        val result = useCase.execute()

        assertEquals(AppResult.Error(err), result)
        coVerify(exactly = 1) { provider.initialize() }
    }
}
