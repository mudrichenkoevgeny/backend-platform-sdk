package io.github.mudrichenkoevgeny.backend.feature.user.usecase.system.auth.settings

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.provider.authsettings.AuthSettingsProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case: initialize auth settings (e.g. seed or reload from storage).
 *
 * Delegates to [AuthSettingsProvider.initialize]. Returns [AppResult.Success] when initialization succeeds or [AppResult.Error] from the provider.
 */
@Singleton
class SeedAuthSettingsUseCase @Inject constructor(
    private val authSettingsProvider: AuthSettingsProvider
) {
    suspend fun execute(): AppResult<Unit> {
        return authSettingsProvider.initialize()
    }
}