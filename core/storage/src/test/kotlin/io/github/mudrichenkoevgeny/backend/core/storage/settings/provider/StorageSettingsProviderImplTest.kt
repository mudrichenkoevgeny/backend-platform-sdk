package io.github.mudrichenkoevgeny.backend.core.storage.settings.provider

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.settings.service.SystemSettingsService
import io.github.mudrichenkoevgeny.backend.core.storage.config.model.createTestStorageConfig
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.storagesettings.ManagementStorageSettings
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class StorageSettingsProviderImplTest {

    private val settingsService = mockk<SystemSettingsService>()
    private val config = createTestStorageConfig()
    private lateinit var provider: StorageSettingsProviderImpl

    @BeforeEach
    fun setup() {
        provider = StorageSettingsProviderImpl(settingsService, config)
    }

    @Test
    fun `initialize registers default storage settings`() = runBlocking {
        coEvery { settingsService.registerDefaults(any()) } returns AppResult.Success(Unit)

        val result = provider.initialize()

        assertTrue(result is AppResult.Success)
        coVerify(exactly = 1) { settingsService.registerDefaults(any()) }
    }

    @Test
    fun `getManagementStorageSettings returns effective settings from system settings service`() {
        every { settingsService.getLong(StorageSettingsProviderImpl.KEY_MIN_UPLOAD_SIZE_BYTES) } returns 1L
        every { settingsService.getLong(StorageSettingsProviderImpl.KEY_MAX_UPLOAD_SIZE_BYTES) } returns 52428800L
        every { settingsService.getString(StorageSettingsProviderImpl.KEY_TOTAL_STORAGE_QUOTA_FOR_OPEN_USER_BYTES) } returns "1073741824"
        every { settingsService.getString(StorageSettingsProviderImpl.KEY_TOTAL_STORAGE_QUOTA_FOR_MANAGEMENT_USER_BYTES) } returns ""
        every { settingsService.getInt(StorageSettingsProviderImpl.KEY_PRESIGNED_URL_EXPIRATION_SECONDS) } returns 900
        every { settingsService.getInt(StorageSettingsProviderImpl.KEY_MAX_PENDING_UPLOADS_FOR_OPEN_USER) } returns 10
        every { settingsService.getInt(StorageSettingsProviderImpl.KEY_MAX_PENDING_UPLOADS_FOR_MANAGEMENT_USER) } returns 100
        every { settingsService.getBoolean(StorageSettingsProviderImpl.KEY_IS_DEDUPLICATION_ENABLED) } returns true
        every { settingsService.getInt(StorageSettingsProviderImpl.KEY_DELETED_FILES_CLEANUP_INTERVAL_SECONDS) } returns 86400

        val settings: ManagementStorageSettings = provider.getManagementStorageSettings()

        assertEquals(1L, settings.minUploadSizeBytes)
        assertEquals(52428800L, settings.maxUploadSizeBytes)
        assertEquals(1073741824L, settings.totalStorageQuotaForOpenUserBytes)
        assertEquals(null, settings.totalStorageQuotaForManagementUserBytes)
        assertEquals(900, settings.presignedUrlExpirationSeconds)
        assertEquals(10, settings.maxPendingUploadsForOpenUser)
        assertEquals(100, settings.maxPendingUploadsForManagementUser)
        assertTrue(settings.isDeduplicationEnabled)
        assertEquals(86400, settings.deletedFilesCleanupIntervalSeconds)
    }
}
