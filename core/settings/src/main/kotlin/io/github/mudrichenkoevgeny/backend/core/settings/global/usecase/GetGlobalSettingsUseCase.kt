package io.github.mudrichenkoevgeny.backend.core.settings.global.usecase

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.settings.global.model.GlobalSettings
import io.github.mudrichenkoevgeny.backend.core.settings.global.provider.GlobalSettingsProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetGlobalSettingsUseCase @Inject constructor(
    private val globalSettingsProvider: GlobalSettingsProvider
) {
    fun execute(): AppResult<GlobalSettings> {
        return globalSettingsProvider.getSettings()
    }
}