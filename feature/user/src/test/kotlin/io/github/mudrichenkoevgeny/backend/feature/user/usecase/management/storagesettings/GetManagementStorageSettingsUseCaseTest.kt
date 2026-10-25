package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.storagesettings

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.storage.settings.provider.StorageSettingsProvider
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.storage.settings.GetManagementStorageSettingsUseCase
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.storagesettings.ManagementStorageSettings
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GetManagementStorageSettingsUseCaseTest {

    private val storageSettingsProvider = mockk<StorageSettingsProvider>()
    private lateinit var useCase: GetManagementStorageSettingsUseCase

    @BeforeEach
    fun setup() {
        useCase = GetManagementStorageSettingsUseCase(storageSettingsProvider)
    }

    @Test
    fun `invoke returns management storage settings successfully`() {
        val expectedSettings = ManagementStorageSettings(
            minUploadSizeBytes = 1L,
            maxUploadSizeBytes = 52428800L,
            totalStorageQuotaForOpenUserBytes = 1073741824L,
            totalStorageQuotaForManagementUserBytes = null,
            presignedUrlExpirationSeconds = 900,
            maxPendingUploadsForOpenUser = 10,
            maxPendingUploadsForManagementUser = 100,
            isDeduplicationEnabled = true,
            deletedFilesCleanupIntervalSeconds = 86400
        )
        every { storageSettingsProvider.getManagementStorageSettings() } returns expectedSettings

        val result = useCase()

        assertTrue(result is AppResult.Success)
        assertEquals(expectedSettings, (result as AppResult.Success).data)
    }
}
