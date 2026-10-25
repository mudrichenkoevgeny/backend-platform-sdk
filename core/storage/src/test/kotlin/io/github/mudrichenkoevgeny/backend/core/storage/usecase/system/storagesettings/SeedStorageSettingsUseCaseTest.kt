package io.github.mudrichenkoevgeny.backend.core.storage.usecase.system.storagesettings

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.storage.settings.provider.StorageSettingsProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SeedStorageSettingsUseCaseTest {

    private val storageSettingsProvider = mockk<StorageSettingsProvider>()
    private lateinit var useCase: SeedStorageSettingsUseCase

    @BeforeEach
    fun setup() {
        useCase = SeedStorageSettingsUseCase(storageSettingsProvider)
    }

    @Test
    fun `invoke calls initialize and returns success`() = runTest {
        coEvery { storageSettingsProvider.initialize() } returns AppResult.Success(Unit)

        val result = useCase()

        assertTrue(result is AppResult.Success)
        coVerify(exactly = 1) { storageSettingsProvider.initialize() }
    }

    @Test
    fun `invoke returns error when initialize fails`() = runTest {
        val expectedError = AppResult.Error(CommonError.Internal(RuntimeException("DB error")))
        coEvery { storageSettingsProvider.initialize() } returns expectedError

        val result = useCase()

        assertEquals(expectedError, result)
        coVerify(exactly = 1) { storageSettingsProvider.initialize() }
    }
}