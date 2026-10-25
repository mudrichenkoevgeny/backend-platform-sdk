package io.github.mudrichenkoevgeny.backend.core.storage.usecase.system.storagesettings

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.storage.settings.provider.StorageSettingsProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SeedStorageSettingsUseCase @Inject constructor(
    private val storageSettingsProvider: StorageSettingsProvider
) {
    /**
     * Seeds initial storage configurations and defaults into the system storage.
     *
     * **Workflow:**
     * 1. Invokes storageSettingsProvider.initialize() to ensure all required storage
     *    settings and policies are registered if they do not already exist.
     *
     * **Execution Context:**
     * - Intended for use during application bootstrap or system initialization.
     * - Does not require a user context as it operates at the system level.
     *
     * @return [AppResult] indicating whether the initialization was successful.
     */
    suspend operator fun invoke(): AppResult<Unit> = storageSettingsProvider.initialize()
}