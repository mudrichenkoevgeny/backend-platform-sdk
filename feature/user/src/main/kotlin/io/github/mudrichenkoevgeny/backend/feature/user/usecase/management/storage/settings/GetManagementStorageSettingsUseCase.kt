package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.storage.settings

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.storage.settings.provider.StorageSettingsProvider
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.storagesettings.ManagementStorageSettings
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Management use case for retrieving an effective snapshot of global storage settings and policies.
 *
 * Exposes platform storage quotas, file size limits, and presigned URL expiration durations to staff and admin clients.
 */
@Singleton
public class GetManagementStorageSettingsUseCase @Inject constructor(
    private val storageSettingsProvider: StorageSettingsProvider
) {
    /**
     * Retrieves a management snapshot of the effective storage settings and limits.
     *
     * **Authorization:**
     * - **Public Access:** Allowed.
     *
     * **Security:**
     * - Provides management storage parameters.
     *
     * **Workflow:**
     * 1. Fetches the current effective management storage settings via [storageSettingsProvider].
     *
     * @return [AppResult] containing the [ManagementStorageSettings].
     */
    public operator fun invoke(): AppResult<ManagementStorageSettings> =
        AppResult.Success(storageSettingsProvider.getManagementStorageSettings())
}
