package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.storage.settings

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.storage.settings.provider.StorageSettingsProvider
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.storagesettings.OpenStorageSettings
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GetOpenStorageSettingsUseCaseTest {

    private val storageSettingsProvider = mockk<StorageSettingsProvider>()
    private lateinit var useCase: GetOpenStorageSettingsUseCase

    @BeforeEach
    fun setup() {
        useCase = GetOpenStorageSettingsUseCase(storageSettingsProvider)
    }

    @Test
    fun `invoke returns open storage settings successfully`() {
        val expectedSettings = OpenStorageSettings(
            minUploadSizeBytes = 1L,
            maxUploadSizeBytes = 52428800L
        )
        every { storageSettingsProvider.getOpenStorageSettings() } returns expectedSettings

        val result = useCase()

        assertTrue(result is AppResult.Success)
        assertEquals(expectedSettings, (result as AppResult.Success).data)
    }
}