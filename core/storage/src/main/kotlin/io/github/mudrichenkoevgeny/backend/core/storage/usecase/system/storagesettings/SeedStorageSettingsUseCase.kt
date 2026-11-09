package io.github.mudrichenkoevgeny.backend.core.storage.usecase.system.storagesettings

import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.storage.settings.provider.StorageSettingsProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * System use case for seeding default storage management configurations into persistent storage.
 *
 * Runs during application bootstrap to ensure platform-wide storage quotas, upload size limits,
 * presigned URL expiration policies, and cleanup intervals exist in system settings.
 */
@Singleton
public class SeedStorageSettingsUseCase @Inject constructor(
    private val storageSettingsProvider: StorageSettingsProvider
) {
    /**
     * Seeds missing default storage management settings and policies into persistent storage.
     *
     * **Allowed Account Statuses:** Any (System initialization).
     *
     * **Security:**
     * - System-level initialization component; no caller authentication or user context required.
     *
     * **Workflow:**
     * 1. Invokes [StorageSettingsProvider.initialize] to register missing storage management settings.
     * 2. Populates default storage quotas, upload limits, and presigned URL expiration policies.
     *
     * @return [AppResult.Success] on successful seeding, or an [AppError] on storage error.
     */
    public suspend operator fun invoke(): AppResult<Unit> = storageSettingsProvider.initialize()
}
