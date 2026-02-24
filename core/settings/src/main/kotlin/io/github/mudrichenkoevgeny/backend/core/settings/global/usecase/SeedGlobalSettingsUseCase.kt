package io.github.mudrichenkoevgeny.backend.core.settings.global.usecase

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.settings.global.provider.GlobalSettingsProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SeedGlobalSettingsUseCase @Inject constructor(
    private val globalSettingsProvider: GlobalSettingsProvider
) {
    suspend fun execute(): AppResult<Unit> {
        return globalSettingsProvider.initialize()
    }
}