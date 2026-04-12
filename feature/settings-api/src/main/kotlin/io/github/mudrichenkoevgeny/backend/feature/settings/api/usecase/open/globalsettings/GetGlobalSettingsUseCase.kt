package io.github.mudrichenkoevgeny.backend.feature.settings.api.usecase.open.globalsettings

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.settings.global.provider.GlobalSettingsProvider
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.domain.model.globalsettings.GlobalSettings
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
    operator fun invoke(): AppResult<GlobalSettings> {
        return globalSettingsProvider.getSettings()
    }
}