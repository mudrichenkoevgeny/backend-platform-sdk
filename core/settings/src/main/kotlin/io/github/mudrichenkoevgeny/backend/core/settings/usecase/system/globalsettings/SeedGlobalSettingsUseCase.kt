package io.github.mudrichenkoevgeny.backend.core.settings.usecase.system.globalsettings

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.settings.global.provider.GlobalSettingsProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for seeding global settings defaults into persistent storage.
 *
 * Delegates to [GlobalSettingsProvider.initialize].
 */
@Singleton
class SeedGlobalSettingsUseCase @Inject constructor(
    private val globalSettingsProvider: GlobalSettingsProvider
) {
    /**
     * Seeds default values (if any) and returns the result.
     */
    suspend operator fun invoke(): AppResult<Unit> {
        return globalSettingsProvider.initialize()
    }
}