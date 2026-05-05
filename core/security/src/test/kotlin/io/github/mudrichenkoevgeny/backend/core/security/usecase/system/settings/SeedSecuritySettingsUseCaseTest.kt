package io.github.mudrichenkoevgeny.backend.core.security.usecase.system.settings

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.settings.provider.SecuritySettingsProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SeedSecuritySettingsUseCaseTest {

    private val securitySettingsProvider = mockk<SecuritySettingsProvider>()
    private lateinit var useCase: SeedSecuritySettingsUseCase

    @BeforeEach
    fun setup() {
        useCase = SeedSecuritySettingsUseCase(securitySettingsProvider)
    }

    @Test
    fun `invoke calls initialize and returns success`() = runTest {
        coEvery { securitySettingsProvider.initialize() } returns AppResult.Success(Unit)

        val result = useCase()

        assertTrue(result is AppResult.Success)
        coVerify(exactly = 1) { securitySettingsProvider.initialize() }
    }

    @Test
    fun `invoke returns error when initialize fails`() = runTest {
        val expectedError = AppResult.Error(CommonError.Internal(RuntimeException("DB error")))
        coEvery { securitySettingsProvider.initialize() } returns expectedError

        val result = useCase()

        assertEquals(expectedError, result)
        coVerify(exactly = 1) { securitySettingsProvider.initialize() }
    }
}