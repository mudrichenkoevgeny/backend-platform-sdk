package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.storage.settings

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.storage.settings.provider.StorageSettingsProvider
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.storagesettings.OpenStorageSettings
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetOpenStorageSettingsUseCase @Inject constructor(
    private val storageSettingsProvider: StorageSettingsProvider
) {
    /**
     * Retrieves a public snapshot of the effective storage settings and limits.
     *
     * **Authorization:**
     * - **Public Access:** Allowed.
     *
     * **Security:**
     * - No authentication required.
     * - Provides open storage parameters.
     *
     * **Workflow:**
     * 1. Fetches the current effective open storage settings via [storageSettingsProvider].
     *
     * @return [AppResult] containing the [OpenStorageSettings].
     */
    operator fun invoke(): AppResult<OpenStorageSettings> {
        return AppResult.Success(storageSettingsProvider.getOpenStorageSettings())
    }
}