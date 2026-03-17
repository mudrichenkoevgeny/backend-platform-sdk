package io.github.mudrichenkoevgeny.backend.core.settings.global.usecase

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.settings.global.model.GlobalSettings
import io.github.mudrichenkoevgeny.backend.core.settings.global.provider.GlobalSettingsProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for retrieving a public snapshot of [GlobalSettings].
 */
@Singleton
class GetGlobalSettingsUseCase @Inject constructor(
    private val globalSettingsProvider: GlobalSettingsProvider
) {
    /**
     * Returns the current global settings snapshot.
     */
    fun execute(): AppResult<GlobalSettings> {
        return globalSettingsProvider.getSettings()
    }
}